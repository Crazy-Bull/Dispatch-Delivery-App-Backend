package com.laioffer.dispatchdeliveryapp.config;

import com.laioffer.dispatchdeliveryapp.repository.SfWayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate; // 🔥 注入 JdbcTemplate 处理原生的过程函数调用
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class MapDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(MapDataInitializer.class);

    @Autowired
    private SfWayRepository sfWayRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 🔥 用于执行底层的拓扑函数调用，断绝语法映射冲突

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:${DATABASE_USERNAME:postgres}}")
    private String dbUsername;

    @Value("${spring.datasource.password:${DATABASE_PASSWORD:secret}}")
    private String dbPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void autoImportAndBuildTopology() {
        log.info("🚀 Spring Boot application initialized. Parsing database metrics...");

        try {
            // 1. Decompose database connection configurations dynamically
            String cleanUrl = datasourceUrl.replace("jdbc:postgresql://", "");
            String hostAndPort = cleanUrl.substring(0, cleanUrl.indexOf("/"));
            String dbHost = hostAndPort.contains(":") ? hostAndPort.split(":")[0] : hostAndPort;
            String dbName = cleanUrl.substring(cleanUrl.indexOf("/") + 1);

            log.info("🎯 Connection properties parsed -> Host: {}, Database: {}, User: {}", dbHost, dbName, dbUsername);

            // 2. Resolve PBF file path within internal project context resources
            File pbfFile = new ClassPathResource("map/SanFrancisco.osm.pbf").getFile();
            String dynamicPbfPath = pbfFile.getAbsolutePath();

            // 3. Match cross-platform binary target formats
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String executableName = isWindows ? "osm2pgsql.exe" : "osm2pgsql";

            // 4. Assemble system pipeline command arguments
            String[] command = {
                    executableName,
                    "-c",
                    "-d", dbName,
                    "-H", dbHost,
                    "-U", dbUsername,
                    dynamicPbfPath
            };

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 5. Shift context path execution space dynamically to prevent default.style errors
            String searchCommand = isWindows ? "where" : "which";
            Process locateProcess = new ProcessBuilder(searchCommand, executableName).start();
            try (BufferedReader locReader = new BufferedReader(new InputStreamReader(locateProcess.getInputStream()))) {
                String binaryAbsolutePath = locReader.readLine();
                if (binaryAbsolutePath != null && !binaryAbsolutePath.isEmpty()) {
                    File binaryFile = new File(binaryAbsolutePath.trim());
                    File binaryDir = binaryFile.getParentFile();
                    if (binaryDir != null && binaryDir.exists()) {
                        processBuilder.directory(binaryDir);
                        log.info("📂 Working runtime environment directory mapped to tool home: {}", binaryDir.getAbsolutePath());
                    }
                }
            }

            // 6. Bind credential password token state securely
            processBuilder.environment().put("PGPASSWORD", dbPassword);
            processBuilder.redirectErrorStream(true);

            log.info("💾 Invoking external helper process tool to stream open-street datasets into PostgreSQL...");
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[osm2pgsql] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("osm2pgsql data ingestion exited with an error code: " + exitCode);
            }
            log.info("✅ Raw OpenStreetMap lines populated into planet_osm_line staging container.");

            // 7. Core graph data step-by-step pipeline execution sequence via repository proxy
            log.info("🔄 Filtering and populating clean spatial ways...");
            sfWayRepository.populateFromPlanetOsmLine();

            log.info("🕸️ Compiling intersection matrices and generating pgRouting topology graph...");

            // 步骤 A：创建一个临时的、纯粹的地图交叉路口空间节点表
            jdbcTemplate.execute("DROP TABLE IF EXISTS sf_ways_vertices_pgr CASCADE");
            jdbcTemplate.execute("""
                CREATE TABLE sf_ways_vertices_pgr (
                    id SERIAL PRIMARY KEY,
                    the_geom geometry(Point, 4326)
                )
            """);

            // 步骤 B：把全城所有道路的“起点坐标”和“终点坐标”全部提取出来，合并去重后作为交叉路口节点
            jdbcTemplate.execute("""
                INSERT INTO sf_ways_vertices_pgr (the_geom)
                SELECT DISTINCT ST_StartPoint(geom) FROM sf_ways
                UNION
                SELECT DISTINCT ST_EndPoint(geom) FROM sf_ways
            """);

            // 步骤 C：为节点表建立空间索引确保极速碰撞
            log.info("⚡ Creating high-performance functional spatial indexes on road endpoints...");

            // 为节点表的几何列建立基础空间索引
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sf_vertices_geom ON sf_ways_vertices_pgr USING gist(the_geom)");

            // 🔥 核心优化：为 sf_ways 表的起点和终点，建立高精度的函数式空间索引
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sf_ways_start_geom ON sf_ways USING gist(ST_StartPoint(geom))");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sf_ways_end_geom ON sf_ways USING gist(ST_EndPoint(geom))");

            log.info("🕸️ Snapping road segments to the generated network intersections topology...");

            // 步骤 D：结合 && 索引预过滤操作符，微秒级完成 source 节点的吸附和映射
            jdbcTemplate.execute("""
                UPDATE sf_ways s
                SET source = v.id
                FROM sf_ways_vertices_pgr v
                WHERE ST_StartPoint(s.geom) && v.the_geom AND ST_Equals(ST_StartPoint(s.geom), v.the_geom)
            """);

            // 微秒级完成 target 节点的吸附和映射
            jdbcTemplate.execute("""
                UPDATE sf_ways s
                SET target = v.id
                FROM sf_ways_vertices_pgr v
                WHERE ST_EndPoint(s.geom) && v.the_geom AND ST_Equals(ST_EndPoint(s.geom), v.the_geom)
            """);

            // =========================================================================
            // 📊 最终步骤：通行成本权重初始化
            // =========================================================================
            log.info("📊 Building analytical network bidirectional cost bounds...");
            jdbcTemplate.execute("UPDATE sf_ways SET cost = ST_Length(geom, true)");
            jdbcTemplate.execute("UPDATE sf_ways SET reverse_cost = CASE WHEN one_way = 'yes' THEN -1 ELSE ST_Length(geom, true) END");

            log.info("🎉 All startup sequence actions finalized successfully. Spatial routing platform active!");

        } catch (Exception e) {
            log.error("❌ Map analytical automation scheduler pipeline crashed during processing: ", e);
        }
    }
}
