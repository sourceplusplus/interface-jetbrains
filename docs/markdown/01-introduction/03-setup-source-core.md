In order to setup Source++ Core you will need:

 * [Java](https://www.oracle.com/java/)
 * [Apache SkyWalking OAP](https://skywalking.apache.org/)

# Install Java (JDK 8)
```sh
apt-get update && sudo apt install -y openjdk-8-jdk
```

# Download Apache SkyWalking OAP
```sh
cd /opt
wget https://www-us.apache.org/dist/skywalking/6.1.0/apache-skywalking-apm-6.1.0.tar.gz
gunzip apache-skywalking-apm-6.1.0.tar.gz
tar -xvf apache-skywalking-apm-6.1.0.tar
rm apache-skywalking-apm-6.1.0.tar
mv apache-skywalking-apm-bin apache-skywalking
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
wget https://github.com/sourceplusplus/Assistant/releases/download/v0.2.1-alpha/source-core-0.2.1.zip
unzip source-core-0.2.1.zip
rm source-core-0.2.1.zip
mv source-core-0.2.1 source-core
```

## Start Source++ Core
```sh
cd /opt/source-core
nohup ./bin/source-core >/dev/null 2>&1 &
```

Source++ Core may take some time to boot. You can verify when it's available by using:
```sh
curl -XGET 'localhost:8080'
```

Once the above doesn't return `connection refused` you have successfully started Source++ Core! Now all you will need to do is integrate the two services together. This can be easily done via the Source++ Plugin.

# Next Step

- [Install Source++ Plugin](./05-install-source-plugin.md) or [Configure Source++ Core](./04-configure-source-core.md)
