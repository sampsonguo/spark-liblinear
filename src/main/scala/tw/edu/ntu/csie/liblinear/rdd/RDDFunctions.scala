package tw.edu.ntu.csie.liblinear.rdd

import scala.language.implicitConversions
import scala.reflect.ClassTag
import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

/**
 * Spark LIBLINEAR specific RDD functions.
 */
class RDDFunctions[T: ClassTag](self: RDD[T])
{
	def slaveReduce(f: (T, T) => T, numSlaves : Int): T = {
		val depth = 2
		var rdd = self
		var numPartitions = rdd.partitions.size
		if(numSlaves > 0)
		{
			rdd = rdd.coalesce(numSlaves, false)
		}
		numPartitions = numSlaves
		val scale = math.max(math.ceil(math.pow(numPartitions, 1.0 / depth)).toInt, 2)
		while (numPartitions > scale + numPartitions / scale)
		{
			numPartitions /= scale
			val curNumPartitions = numPartitions
			rdd = rdd.mapPartitionsWithIndex{(i, iter) =>
				iter.map((i % curNumPartitions, _))
			}.reduceByKey(new HashPartitioner(curNumPartitions), f).values
		}
		rdd.reduce(f)
	}
}

object RDDFunctions
{

	/** Implicit conversion from an RDD to RDDFunctions. */
	implicit def fromRDD[T: ClassTag](rdd: RDD[T]) = new RDDFunctions[T](rdd)
}
