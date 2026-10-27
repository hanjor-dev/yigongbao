package com.yigongbao.module.backup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoMapperXmlTest {

    @Test
    void moduleDoesNotContainMapperXml() throws IOException {
        try (var files = Files.walk(Path.of("src"))) {
            List<Path> mapperXmlFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Mapper.xml"))
                    .toList();

            assertThat(mapperXmlFiles)
                    .as("备份模块禁止新增 Mapper XML，应使用 MyBatis-Plus 注解或 Java Mapper")
                    .isEmpty();
        }
    }
}
