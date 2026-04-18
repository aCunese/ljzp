package com.example.Kcsj.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.User;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.mapper.CameraRecordsMapper;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import com.example.Kcsj.mapper.UserMapper;
import com.example.Kcsj.mapper.VideoRecordsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Seed basic demo data for records/avatar so the UI has visible content on first launch.
 */
@Component
@RequiredArgsConstructor
@Order(30)
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEFAULT_AVATAR = "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif";
    private static final String EXTERNAL_DEMO_VIDEO_URL = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ImgRecordsMapper imgRecordsMapper;
    private final VideoRecordsMapper videoRecordsMapper;
    private final CameraRecordsMapper cameraRecordsMapper;
    private final UserMapper userMapper;

    @Value("${app.seed-demo-records:true}")
    private boolean seedDemoRecords;

    @Value("${app.seed-default-avatar:true}")
    private boolean seedDefaultAvatar;

    @Value("${file.ip:localhost}")
    private String fileIp;

    @Value("${server.port:9999}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Override
    public void run(String... args) {
        if (seedDefaultAvatar) {
            ensureDefaultAvatar();
        }

        if (!seedDemoRecords) {
            log.info("Demo record seeding disabled.");
            return;
        }

        seedImageRecordIfEmpty();
        seedVideoRecordIfEmpty();
        seedCameraRecordIfEmpty();
    }

    private void ensureDefaultAvatar() {
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery().isNull(User::getAvatar)
        );
        if (users.isEmpty()) {
            return;
        }
        users.forEach(user -> {
            user.setAvatar(DEFAULT_AVATAR);
            userMapper.updateById(user);
        });
        log.info("Filled default avatar for {} user(s).", users.size());
    }

    private void seedImageRecordIfEmpty() {
        Integer count = imgRecordsMapper.selectCount(Wrappers.emptyWrapper());
        if (count != null && count > 0) {
            return;
        }

        String sampleImage = resolveSampleImageUrl();
        String now = LocalDateTime.now().format(TIME_FORMATTER);

        ImgRecords demo = new ImgRecords();
        demo.setUsername("admin");
        demo.setKind("Corn");
        demo.setWeight("yolov11n.pt");
        demo.setConf("0.25");
        demo.setStartTime(now);
        demo.setInputImg(sampleImage);
        demo.setOutImg(sampleImage);
        demo.setConfidence("[0.93]");
        demo.setLable("[\"CORN_VIRAL_DISEASE\"]");
        demo.setAllTime("0.42s");

        imgRecordsMapper.insert(demo);
        log.info("Seeded 1 demo image recognition record.");
    }

    private void seedVideoRecordIfEmpty() {
        Integer count = videoRecordsMapper.selectCount(Wrappers.emptyWrapper());
        if (count != null && count > 0) {
            return;
        }

        String now = LocalDateTime.now().format(TIME_FORMATTER);
        String sampleVideo = resolveSampleVideoUrl();
        VideoRecords demo = new VideoRecords();
        demo.setUsername("admin");
        demo.setKind("Corn");
        demo.setWeight("yolov11n.pt");
        demo.setConf("0.25");
        demo.setStartTime(now);
        demo.setInputVideo(sampleVideo);
        demo.setOutVideo(sampleVideo);

        videoRecordsMapper.insert(demo);
        log.info("Seeded 1 demo video recognition record.");
    }

    private void seedCameraRecordIfEmpty() {
        Integer count = cameraRecordsMapper.selectCount(Wrappers.emptyWrapper());
        if (count != null && count > 0) {
            return;
        }

        String now = LocalDateTime.now().format(TIME_FORMATTER);
        String sampleVideo = resolveSampleVideoUrl();
        CameraRecords demo = new CameraRecords();
        demo.setUsername("admin");
        demo.setKind("Corn");
        demo.setWeight("yolov11n.pt");
        demo.setConf("0.25");
        demo.setStartTime(now);
        demo.setOutVideo(sampleVideo);

        cameraRecordsMapper.insert(demo);
        log.info("Seeded 1 demo camera recognition record.");
    }

    private String resolveSampleImageUrl() {
        Path filesDir = Paths.get(System.getProperty("user.dir"), "files");
        if (Files.exists(filesDir) && Files.isDirectory(filesDir)) {
            try (Stream<Path> stream = Files.list(filesDir)) {
                Optional<Path> firstImage = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> isImageFile(path.getFileName().toString()))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .findFirst();
                if (firstImage.isPresent()) {
                    String fileName = firstImage.get().getFileName().toString();
                    String flag = resolveFileFlag(fileName);
                    return buildFileUrl(flag);
                }
            } catch (Exception e) {
                log.warn("Failed to scan local files directory for demo image: {}", e.getMessage());
            }
        }
        return "https://picsum.photos/seed/njzp-demo/960/540";
    }

    private String resolveSampleVideoUrl() {
        Path filesDir = Paths.get(System.getProperty("user.dir"), "files");
        if (Files.exists(filesDir) && Files.isDirectory(filesDir)) {
            try (Stream<Path> stream = Files.list(filesDir)) {
                Optional<Path> firstVideo = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> isVideoFile(path.getFileName().toString()))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .findFirst();
                if (firstVideo.isPresent()) {
                    String fileName = firstVideo.get().getFileName().toString();
                    String flag = resolveFileFlag(fileName);
                    return buildFileUrl(flag);
                }
            } catch (Exception e) {
                log.warn("Failed to scan local files directory for demo video: {}", e.getMessage());
            }
        }
        return EXTERNAL_DEMO_VIDEO_URL;
    }

    private String resolveFileFlag(String fileName) {
        String baseName = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
        }
        int underscoreIndex = baseName.indexOf('_');
        if (underscoreIndex > 0) {
            String maybeUuid = baseName.substring(0, underscoreIndex);
            if (maybeUuid.matches("^[a-fA-F0-9]{32}$")) {
                return maybeUuid;
            }
        }
        return baseName;
    }

    private String buildFileUrl(String flag) {
        String normalizedContext = contextPath == null ? "" : contextPath.trim();
        if (!normalizedContext.startsWith("/")) {
            normalizedContext = "/" + normalizedContext;
        }
        if ("/".equals(normalizedContext)) {
            normalizedContext = "";
        }
        return String.format("http://%s:%s%s/files/%s", fileIp, serverPort, normalizedContext, flag);
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    private boolean isVideoFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov")
                || lower.endsWith(".mkv") || lower.endsWith(".webm");
    }
}
