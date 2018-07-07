package cn.ocoop.framework.id;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(name = "id.key")
public class IdWorkAutoConfiguration {

    public IdWorkAutoConfiguration(StringRedisTemplate redisTemplate, Environment environment) throws IOException {


        Long workerId = getLocalWorkId();
        try {
            while (workerId == null) {
                Long newWorkId = redisTemplate.opsForValue().increment(environment.getProperty("id.key"), 1);
                saveLocalWorkId(String.valueOf(newWorkId));
                workerId = getLocalWorkId();
            }
        } catch (Throwable e) {
            log.error("获取workID失败", e);
            throw e;
        }

        if (workerId > Id.WORKER_ID_MAX_VALUE) {
            throw new RuntimeException("超过最大启动实例");
        }

        Id.WORKER_ID = workerId;
    }


    private void saveLocalWorkId(String workId) throws IOException {
        File workIdHome = getWorkIdHome();
        FileUtils.writeStringToFile(new File(workIdHome.getAbsoluteFile() + "/" + UUID.randomUUID() + ".lock"), workId, Charsets.UTF_8);
    }

    private File getWorkIdHome() {
        return new File(FileUtils.getUserDirectoryPath() + "/.workId");
    }


    private Long getLocalWorkId() throws IOException {
        File workIdHome = getWorkIdHome();
        if (!workIdHome.exists()) return null;

        Collection<File> files = FileUtils.listFiles(workIdHome, new String[]{"lock"}, false);
        if (CollectionUtils.isEmpty(files)) return null;

        for (File file : files) {
            FileChannel channel = null;
            FileLock fileLock = null;
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                channel = randomAccessFile.getChannel();
                fileLock = channel.tryLock();
                if (fileLock != null) return Long.valueOf(randomAccessFile.readLine());
            } finally {
                if (fileLock == null) {
                    if (channel != null) channel.close();
                } else {
                    final FileChannel fileChannelFinal = channel;
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            fileChannelFinal.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
                }
            }

        }

        return null;
    }


}
