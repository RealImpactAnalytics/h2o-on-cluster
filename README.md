Setting up an Amazon cluster
====

First 

    git clone https://github.com/amplab/spark-ec2.git
    
Login into to the Amazon console UI and create a Key-Pair (kp)
    
cd into the repo then run
        
    ./spark-ec2 --key-pair=kp --identity-file=/path/kp.pem --region=eu-west-1 --copy-aws-credentials --hadoop-major-version=yarn -s 2 launch h2o

If everything went fine you would be able to connect to the Spark cluster UI `http://MASTER-IP:8080`

you might want to 
    
    chmod 400 /path/kp.pem

Once the cluster is fully set up add a rule into the security group of the master node of the cluster

    Custom TCP Rule     TCP     7077    0.0.0.0/0

Build the App
====

from the root of the project

    sbt assembly 
    
then 

    spark-submit 

Run app on cluster
====

run from the local app repo

    scp -i /path/kv.pem target/scala-2.10/h2oOnCluster-assembly-1.0.jar root@MASTER-IP:/root/h2o/h2o.jar
    
then ssh into the cluster master node

    ssh -i /path/kv.pem root@MASTER-IP
   
export your AWS credential 

    export AWS_ACCESS_KEY_ID=blabla
    export AWS_SECRET_ACCESS_KEY=blablabla
    
Eventually run    
    
    ./spark/bin/spark-submit --name "H2O test" --master spark://MASTER-IP:7077 h2o/h2o.jar
