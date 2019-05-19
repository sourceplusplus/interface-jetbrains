In order to setup Source++ Core you will need:

 * [Java](https://www.oracle.com/java/)
 * [Elasticsearch](https://www.elastic.co/products/elasticsearch)
 * [Apache SkyWalking OAP](https://skywalking.apache.org/)

# Install Java (JDK 8)
```sh
apt-get update && sudo apt install -y openjdk-8-jdk
```

# Install Elasticsearch

```sh
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.5.4.deb
sudo dpkg -i elasticsearch-6.5.4.deb
sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable elasticsearch.service
sudo /bin/systemctl start elasticsearch.service
rm elasticsearch-6.5.4.deb
```

Elasticsearch may take some time to boot. You can verify when it's available by using:
```sh
curl -XGET 'localhost:9200'
```

Once the above doesn't return `connection refused` you may continue setup.

# Download Apache SkyWalking OAP
```sh
cd /opt
wget https://www-eu.apache.org/dist/incubator/skywalking/6.1.0/apache-skywalking-apm-incubating-6.1.0.tar.gz
gunzip apache-skywalking-apm-incubating-6.1.0.tar.gz
tar -xvf apache-skywalking-apm-incubating-6.1.0.tar
rm apache-skywalking-apm-incubating-6.1.0.tar
mv apache-skywalking-apm-incubating apache-skywalking
```

## Start Apache SkyWalking OAP
```sh
cd /opt/apache-skywalking/bin
./oapService.sh
```

Apache SkyWalking OAP may take some time to boot. You can verify when it's available by using:
```sh
curl -XGET 'localhost:12800'
```

Once the above doesn't return `connection refused` you can continue setup.

# Download Source++ Core
```sh
cd /opt
wget https://github.com/CodeBrig/Source/releases/download/v0.2.0-alpha/source-core-0.2.0.tar
tar -xvf source-core-0.2.0.tar
rm source-core-0.2.0.tar
mv source-core-0.2.0 source-core
```

## Start Source++ Core
```sh
cd /opt/source-core
export SOURCE_CONFIG=/opt/source-core/config/source-core.json
nohup ./bin/source-core >/dev/null 2>&1 &
```

Source++ Core may take some time to boot. You can verify when it's available by using:
```sh
curl -XGET 'localhost:8080'
```

Once the above doesn't return `connection refused` you have successfully finished setting up Source++ Core!

# Next Step

- [Configure Source++ Core](./04-configure-source-core.md)
