#!/usr/bin/env python3
import json
import re
import time
from pathlib import Path
from typing import Dict, List

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

API = "https://commons.wikimedia.org/w/api.php"
UA = "Mozilla/5.0 (compatible; NJZP-SampleFetcher/1.0; +https://commons.wikimedia.org/)"

ROOT = Path('/Users/ruyne./Desktop/ljzp/njzp.tech')
FILES_DIR = ROOT / 'yolo_cropDisease_detection_springboot' / 'files'
FILES_DIR.mkdir(parents=True, exist_ok=True)

IMAGE_QUERIES = [
    ("rice_blast", "rice blast leaf disease"),
    ("rice_sheath_blight", "rice sheath blight disease"),
    ("corn_rust", "corn rust leaf disease"),
    ("corn_leaf_blight", "corn leaf blight disease"),
    ("tomato_late_blight", "tomato late blight leaf"),
    ("tomato_early_blight", "tomato early blight leaf"),
    ("strawberry_powdery_mildew", "strawberry powdery mildew leaf"),
    ("strawberry_leaf_spot", "strawberry leaf spot disease"),
    ("citrus_canker", "citrus canker leaf disease"),
    ("citrus_anthracnose", "citrus anthracnose leaf"),
]

VIDEO_QUERIES = [
    ("rice_video", "rice field video"),
    ("corn_video", "corn field video"),
    ("tomato_video", "tomato plant video"),
    ("strawberry_video", "strawberry farm video"),
    ("citrus_video", "citrus orchard video"),
]

EXT_BY_MIME = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "image/gif": "gif",
    "video/webm": "webm",
    "video/ogg": "ogv",
    "video/mp4": "mp4",
}


def build_session() -> requests.Session:
    s = requests.Session()
    s.headers.update({"User-Agent": UA})
    retry = Retry(
        total=4,
        read=4,
        connect=4,
        backoff_factor=1.2,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["GET"],
    )
    adapter = HTTPAdapter(max_retries=retry)
    s.mount("https://", adapter)
    s.mount("http://", adapter)
    return s


def search_commons_files(session: requests.Session, query: str, limit: int = 30) -> List[Dict]:
    params = {
        "action": "query",
        "generator": "search",
        "gsrsearch": query,
        "gsrnamespace": 6,
        "gsrlimit": str(limit),
        "prop": "imageinfo",
        "iiprop": "url|mime|size|extmetadata",
        "iiurlwidth": "1400",
        "format": "json",
        "formatversion": "2",
    }
    resp = session.get(API, params=params, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    pages = data.get("query", {}).get("pages", [])

    out = []
    for p in pages:
        infos = p.get("imageinfo") or []
        if not infos:
            continue
        ii = infos[0]
        out.append({
            "title": p.get("title", ""),
            "descriptionurl": ii.get("descriptionurl", ""),
            "url": ii.get("url", ""),
            "thumburl": ii.get("thumburl", ""),
            "mime": ii.get("mime", ""),
            "size": ii.get("size", 0),
            "license": (ii.get("extmetadata", {}).get("LicenseShortName", {}) or {}).get("value", ""),
        })
    return out


def choose_ext(item: Dict, is_video: bool) -> str:
    mime = item.get("mime", "")
    if mime in EXT_BY_MIME:
        return EXT_BY_MIME[mime]
    title = item.get("title", "")
    if "." in title:
        ext = title.rsplit(".", 1)[-1].lower()
        if re.fullmatch(r"[a-z0-9]{2,5}", ext):
            return ext
    return "webm" if is_video else "jpg"


def download_binary(session: requests.Session, url: str, dest: Path, max_bytes: int) -> bool:
    try:
        with session.get(url, timeout=60, stream=True) as resp:
            resp.raise_for_status()
            total = 0
            with dest.open("wb") as f:
                for chunk in resp.iter_content(chunk_size=64 * 1024):
                    if not chunk:
                        continue
                    total += len(chunk)
                    if total > max_bytes:
                        f.close()
                        dest.unlink(missing_ok=True)
                        return False
                    f.write(chunk)
        return True
    except Exception:
        dest.unlink(missing_ok=True)
        return False


def main():
    session = build_session()
    manifest = {
        "generated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "source": "Wikimedia Commons API",
        "images": [],
        "videos": [],
    }

    # fetch images: up to 2 per query
    for tag, query in IMAGE_QUERIES:
        got = 0
        try:
            items = search_commons_files(session, query, limit=40)
        except Exception:
            items = []
        for item in items:
            mime = item.get("mime", "")
            if not mime.startswith("image/"):
                continue
            ext = choose_ext(item, is_video=False)
            if ext not in {"jpg", "jpeg", "png", "webp", "gif"}:
                continue
            src = item.get("thumburl") or item.get("url")
            if not src:
                continue
            filename = f"real_{tag}_{got+1:02d}.{ext.replace('jpeg', 'jpg')}"
            dest = FILES_DIR / filename
            if not download_binary(session, src, dest, max_bytes=8 * 1024 * 1024):
                continue
            manifest["images"].append({
                "file": filename,
                "query": query,
                "title": item.get("title"),
                "descriptionurl": item.get("descriptionurl"),
                "license": item.get("license"),
                "mime": mime,
            })
            got += 1
            if got >= 2:
                break

    # fetch videos: up to 1 per query
    for tag, query in VIDEO_QUERIES:
        got = 0
        try:
            items = search_commons_files(session, query, limit=45)
        except Exception:
            items = []
        for item in items:
            mime = item.get("mime", "")
            if not mime.startswith("video/"):
                continue
            ext = choose_ext(item, is_video=True)
            if ext not in {"webm", "ogv", "mp4"}:
                continue
            src = item.get("url")
            if not src:
                continue
            filename = f"real_{tag}_{got+1:02d}.{ext}"
            dest = FILES_DIR / filename
            if not download_binary(session, src, dest, max_bytes=40 * 1024 * 1024):
                continue
            manifest["videos"].append({
                "file": filename,
                "query": query,
                "title": item.get("title"),
                "descriptionurl": item.get("descriptionurl"),
                "license": item.get("license"),
                "mime": mime,
            })
            got += 1
            if got >= 1:
                break

    manifest_path = FILES_DIR / "web_samples_manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Downloaded images: {len(manifest['images'])}")
    print(f"Downloaded videos: {len(manifest['videos'])}")
    print(f"Manifest: {manifest_path}")


if __name__ == "__main__":
    main()
