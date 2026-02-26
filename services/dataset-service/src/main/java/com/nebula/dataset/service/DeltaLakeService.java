package com.nebula.dataset.service;

import io.delta.tables.DeltaTable;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeltaLakeService {

  private final SparkSession spark;

  public DeltaLakeService(SparkSession spark) {
    this.spark = spark;
  }

  public void createEmptyTable(String path, StructType schema) {
    // Create an empty dataframe with the given schema
    Dataset<Row> emptyDf = spark.createDataFrame(spark.emptyDataFrame().rdd(), schema);

    // Write it out as a delta table to initialize it
    emptyDf.write()
        .format("delta")
        .mode("ignore")
        .save(path);
  }

  public boolean tableExists(String path) {
    return DeltaTable.isDeltaTable(spark, path);
  }

  public StructType getSchema(String path) {
    return spark.read().format("delta").load(path).schema();
  }

  public String getSchemaJson(String path) {
    return getSchema(path).json();
  }

  public List<DeltaHistoryInfo> getHistory(String path) {
    DeltaTable table = DeltaTable.forPath(spark, path);
    Dataset<Row> history = table.history();

    return history.collectAsList().stream().map(row -> {
      Long version = row.getAs("version");
      java.sql.Timestamp timestamp = row.getAs("timestamp");
      String operation = row.getAs("operation");
      // The operation parameters are returned as a map
      java.util.Map<String, String> opParams = row.getAs("operationParameters");

      return new DeltaHistoryInfo(version, timestamp.getTime(), operation, opParams);
    }).collect(Collectors.toList());
  }

  public Dataset<Row> readVersion(String path, long version) {
    return spark.read().format("delta").option("versionAsOf", version).load(path);
  }

  public record DeltaHistoryInfo(Long version, Long timestamp, String operation,
      java.util.Map<String, String> operationParameters) {
  }
}
