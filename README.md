# Open Educational Resources (OER) World Map

![Travis CI](https://travis-ci.org/hbz/oerworldmap.svg)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=oerworldmap.org)](https://sonarqube.com/dashboard?id=oerworldmap.org)

For inital background information about this project please refer to the
[Request for Proposals](http://www.hewlett.org/sites/default/files/OER%20mapping%20RFP_Phase%202%20Final%20June%2023%202014.pdf).

## Setup project

### Get Source

    $ git clone git@github.com:hbz/oerworldmap.git
    $ npm install
    $ git submodule init
    $ git submodule update

### Setup Elasticsearch

#### [Download and install elasticsearch](http://www.elasticsearch.org/overview/elkdownloads/)

    $ cd third-party
    $ wget https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.4.1/elasticsearch-2.4.1.zip
    $ unzip elasticsearch-2.4.1.zip
    $ cd elasticsearch-2.4.1
    $ bin/plugin install 'http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-plugin-bundle/2.4.1.0/elasticsearch-plugin-bundle-2.4.1.0-plugin.zip'
    $ bin/elasticsearch

Check with `curl -X GET http://localhost:9200/` if all is well.

#### Configure elasticsearch

Enable dynamic scripting by adding

    script.inline: true
    script.indexed: true

to your `third-party/elasticsearch-2.4.1/config/elasticsearch.yml`.

If you are in an environment where your instance of elasticsearch won't be the only one on the network, you might want
to configure your cluster name to be different from the default `elasticsearch`. To do so, shut down elasticsearch and
edit `cluster.name` in `third-party/elasticsearch-2.4.1/config/elasticsearch.yml` and `es.cluster.name`
in `conf/application.conf` before restarting.

#### Create and configure oerworldmap index (as specified in `es.index.app.name` in `conf/application.conf`)

    $ curl -X PUT http://localhost:9200/oerworldmap/ -d @conf/index-config.json

#### If you're caught with some kind of buggy index during development, simply delete the index and re-create:

    $ curl -X DELETE http://localhost:9200/oerworldmap/
    $ curl -X PUT http://localhost:9200/oerworldmap/ -d @conf/index-config.json

#### Optionally, you may want to [install the head plugin](https://github.com/mobz/elasticsearch-head)

    $ cd third-party/elasticsearch-2.4.1
    $ bin/plugin install mobz/elasticsearch-head

#### Set up Apache

    $ sudo apt-get install apache2
    $ sudo a2enmod proxy proxy_html proxy_http rewrite  auth_basic authz_groupfile

    $ mkdir data/auth
    $ cd data/auth
    $ touch htpasswd htgroups htprofiles

Edit `sudo visudo` and add permission to the user

    username  ALL = NOPASSWD: /usr/sbin/apache2ctl

Modify the path in `conf/auth.conf`

    Include /home/username/oerworldmap/data/permissions/

Enalbe the site

    $ sudo ln -s /home/username/Graphthinking/oerworldmap/conf/auth.conf /etc/apache2/sites-available/
    $ sudo a2ensite auth.conf
    $ sudo apache2ctrl graceful


Modify the path in `data/permissions/.system`

    AuthUserFile /home/username/oerworldmap/data/auth/htpasswd
    AuthGroupFile /home/username/oerworldmap/data/auth/htgroups

Set up the hostname in `/etc/hosts`

    127.0.0.1	localhost oerworldmap.local


### Create database histories

    $ mkdir -p data/consents/objects
    $ touch data/consents/history
    $ mkdir -p data/commits/objects/
    $ mkdir data/commits/history


### Setup Play! Application

Download [sbt](http://www.scala-sbt.org/download.html), then

    $ sbt run

### Work with IDEs

Using [activator](http://www.lightbend.com/community/core-tools/activator-and-sbt), integration to Eclipse and IDEA IntelliJ is provided by running `eclipse` or `idea` from within activator. To run the OER World Map JUnit tests inside IntelliJ, it is necessary to set the test's working directory to the root directory of this project (i. e. `oerworldmap`):

    Run | Edit configurations... | JUnit | <MyTest> | Configuration | Working directory:
    <absolute/path/to/oerworldmap>

## Contribute

### Coding conventions

Indent blocks by *two spaces* and wrap lines at *100 characters*. For more
details, refer to the [Google Java Style
Guide](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).

### Bug reports

Please file bugs as an issue labeled "Bug" [here](https://github.com/hbz/oerworldmap/issues/new). Include browser information and screenshot(s) when applicable.

## Attributions

This product includes GeoLite2 data created by MaxMind, available from
<a href="http://www.maxmind.com">http://www.maxmind.com</a>.
