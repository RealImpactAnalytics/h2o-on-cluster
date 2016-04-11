import org.apache.spark.h2o.H2OContext
import org.apache.spark.sql.DataFrame
import org.apache.spark.h2o._
import hex.tree.drf.{DRFModel, DRF}
import hex.tree.drf.DRFModel.DRFParameters
import org.apache.spark.sql.types.{BooleanType, StringType}
import water.app.ModelMetricsSupport
import water.app.ModelSerializationSupport

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.SQLContext

import scala.sys.env

object H2oOnCluster extends App {
  println("TEST: All is well!")

  /*val SPARK_MASTER = env.getOrElse("SPARK_MASTER", "local[*]" )
  val SOURCES_FILES = env.getOrElse("SOURCES_FILES", "/data" ) // Catch error if not provided

  val conf = new SparkConf().setAppName("H2O on Cluster")
  conf.setMaster(SPARK_MASTER)

  val sc = new SparkContext(conf)*/

  val sc = new SparkContext(new SparkConf())

  println("Used AWS_SECRET_ACCESS_KEY: " + sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", ""))
  println("Used AWS_ACCESS_KEY_ID: " + sys.env.getOrElse("AWS_ACCESS_KEY_ID", ""))

  val hadoopConf = sc.hadoopConfiguration
  hadoopConf.set("fs.s3n.awsSecretAccessKey", sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", ""))
  hadoopConf.set("fs.s3n.awsAccessKeyId", sys.env.getOrElse("AWS_ACCESS_KEY_ID", ""))
  hadoopConf.set("fs.s3n.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")

  val LOG_LEVEL= env.getOrElse("LOG_LEVEL", "ERROR")
  sc.setLogLevel( LOG_LEVEL )
  val sqlContext = new SQLContext(sc)

  val h2oContext = H2OContext.getOrCreate( sc )
  import h2oContext._

  val df = sqlContext.read.parquet( "s3n://h2o-data/adult.parquet" )
  df.show

  val split = df.randomSplit( Array( 0.6, 0.2, 0.2 ) )
  val ( trainFrame, validationFrame, testFrame ) = (
    h2oContext.asH2OFrame( split( 0 ) ),
    h2oContext.asH2OFrame( split( 1 ) ),
    h2oContext.asH2OFrame( split( 2 ) )
  )

  val enumFields = df.schema
    .filter( field => field.dataType.equals( StringType ) || field.dataType.equals( BooleanType ) )
    .map( _.name )

  trainFrame.colToEnum( enumFields.toArray )
  testFrame.colToEnum( enumFields.toArray )

  val drfParams = new DRFParameters()
  drfParams._train = trainFrame
  drfParams._valid = validationFrame
  drfParams._response_column = "incomeGt50K"
  drfParams._ntrees = 50
  drfParams._max_depth = 6

  val drf = new DRF( drfParams )
  val drfModel = drf.trainModel.get
  //ModelSerializationSupport.exportH2OModel( drfModel, modelReference )
  //val model = ModelSerializationSupport.loadH2OModel[DRFModel]( modelReference )

  val prediction = drfModel.score( testFrame )

  val frameWithPrediction = testFrame.add( prediction )

  val frameWithPredictionDf = asDataFrame( frameWithPrediction )( sqlContext )

  frameWithPredictionDf.show

  frameWithPredictionDf.write.parquet( "s3n://h2o-data/prediction.parquet" )

  // Collect model metrics and evaluate model quality
  val trainMetrics = ModelMetricsSupport.binomialMM(drfModel, trainFrame)
  val validMetrics = ModelMetricsSupport.binomialMM(drfModel, validationFrame)
  println("train AUC = " + trainMetrics.auc)
  println("Validation AUC = " + validMetrics.auc)

  Seq( trainFrame, validationFrame, testFrame ).foreach( _.delete )

  println("Awesome")

  sc.stop()
}
