# Open Educational Resources (OER) World Map

![Travis CI](https://travis-ci.org/hbz/oerworldmap.svg)

For inital background information about this project please refer to the
[Request for
  Proposals](http://www.hewlett.org/sites/default/files/OER%20mapping%20RFP_Phase%202%20Final%20June%2023%202014.pdf).

## Setup project

### Setup Elasticsearch

Install Elasticsearch and its head plugin:
    
	wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.3.6.deb
	sudo dpkg -i elasticsearch-1.3.6.deb 
	sudo update-rc.d elasticsearch defaults 95 10
	cd /usr/share/elasticsearch/
	sudo bin/plugin -install mobz/elasticsearch-head

In our project, the elasticsearch settings are defined in application.conf. Accordingly, two values need to be specified in Elasticsearch's overall setup explicitly:

First, in elasticsearch.yml (usually around line 32), set:

	cluster.name: oerworldmaps
	 
(You will find elasticsearch.yml at /etc/elasticsearch/elasticsearch.yml on Ubuntu.)
Start Elasticsearch by

	sudo service elasticsearch start

Second, browse the elasticsearch head plugin by http://localhost:9200/_plugin/head/ --> tab "Indices" --> create a "New Index": "oerworldmap".

### Setup Play! Application

Download [sbt](http://www.scala-sbt.org/download.html), then

    $ sbt run

## Contribute

### Coding conventions

Indent blocks by *two spaces* and wrap lines at *100 characters*. For more
details, refer to the [Google Java Style
Guide](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).
