package com.nebula.dataset.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class SparkConfig {

  @Value("${spark.master}")
  private String sparkMaster;

  @Value("${minio.endpoint}")
  private String minioEndpoint;

  @Value("${minio.accessKey}")
  private String minioAccessKey;

  @Value("${minio.secretKey}")
  private String minioSecretKey;

  private SparkSession sparkSession;

  @Bean
  public SparkSession sparkSession() {
    sparkSession = SparkSession.builder()
        .appName("dataset-service-metadata")
        .master(sparkMaster)
        // Delta Lake configurations
        .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
        .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")

        // Keep memory usage minimal
        .config("spark.driver.memory", "1g")
        .config("spark.sql.shuffle.partitions", "1")
        .config("spark.ui.enabled", "false")

        // Hadoop S3A configurations for MinIO
        .config("spark.hadoop.fs.s3a.endpoint", minioEndpoint)
        .config("spark.hadoop.fs.s3a.access.key", minioAccessKey)
        .config("spark.hadoop.fs.s3a.secret.key", minioSecretKey)
        .config("spark.hadoop.fs.s3a.path.style.access", "true")
        .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
        .config("spark.hadoop.fs.s3a.connection.ssl.enabled", "false")
        .getOrCreate();

    return sparkSession;
  }

  @PreDestroy
  public void closeSparkSession() {
    if (sparkSession != null) {
      sparkSession.stop();
    }
  }
}
