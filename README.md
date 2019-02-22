# Open Educational Resources (OER) World Map

![Travis CI](https://travis-ci.org/hbz/oerworldmap.svg)

For inital background information about this project please refer to the
[Request for Proposals](http://www.hewlett.org/sites/default/files/OER%20mapping%20RFP_Phase%202%20Final%20June%2023%202014.pdf).

## Setup project

### Get Source

    $ git clone git@github.com:hbz/oerworldmap.git

### Create configuration

    $ cp conf/application.example.conf conf/application.conf

### Setup Elasticsearch

#### [Download and install elasticsearch](http://www.elasticsearch.org/overview/elkdownloads/)

    $ cd third-party
    $ wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.1.zip
    $ unzip elasticsearch-6.2.1.zip
    $ cd elasticsearch-6.2.1
    $ bin/elasticsearch-plugin install analysis-icu
    $ bin/elasticsearch

Check with `curl -X GET http://localhost:9200/` if all is well.

Optionally, you may want to [use the head plugin](https://www.elastic.co/blog/running-site-plugins-with-elasticsearch-5-0).
This basically comes down to

    $ cd .. # back to oerworldmap/third-party or choose any directory outside this project
    $ git clone git://github.com/mobz/elasticsearch-head.git
    $ cd elasticsearch-head
    $ npm install
    $ npm run start
    $ open http://localhost:9100/

#### Configure elasticsearch

If you are in an environment where your instance of elasticsearch won't be the only one on the network, you might want
to configure your cluster name to be different from the default `elasticsearch`. To do so, shut down elasticsearch and
edit `cluster.name` in `third-party/elasticsearch-2.4.1/config/elasticsearch.yml` and `es.cluster.name`
in `conf/application.conf` before restarting.

#### Create and configure oerworldmap index (as specified in `es.index.app.name` in `conf/application.conf`)

    $ curl -H "Content-type: application/json" -X PUT http://localhost:9200/oerworldmap/ -d @conf/index-config.json

#### If you're caught with some kind of buggy index during development, simply delete the index and re-create:

    $ curl -X DELETE http://localhost:9200/oerworldmap/
    $ curl -X PUT http://localhost:9200/oerworldmap/ -d @conf/index-config.json

#### Set up Keycloak

bin/standalone.sh -Dkeycloak.profile.feature.scripts=enabled

#### Set up Apache

    $ sudo apt-get install apache2
    $ sudo a2enmod proxy proxy_html proxy_http rewrite  auth_basic authz_groupfile ssl

    $ mkdir data/auth
    $ cd data/auth
    $ touch htpasswd htgroups htprofiles

Edit `sudo visudo` and add permission to the user

    username  ALL = NOPASSWD: /usr/sbin/apache2ctl

Configure variables for `conf/vhost.conf`

    Define PUBLIC_HOST oerworldmap.localhost
    Define PUBLIC_PORT 80
    Define PUBLIC_EMAIL webmaster@oerworldmap.localhost
    Define AUTH_DIR /home/fo/local/src/oerworldmap/data
    Define API_HOST http://localhost:9000
    Define UI_HOST http://localhost:3000
    Define KIBANA_HOST http://localhost:5601
    Define PAGES_HOST http://localhost:4000
    #Define SSL_CIPHER_SUITE
    #Define SSL_CERT_FILE
    #Define SSL_CERT_KEY_FILE
    #Define SSL_CERT_CHAIN_FILE

Enable the site

    $ sudo ln -s /home/username/oerworldmap/conf/vhost.conf /etc/apache2/sites-available/oerworldmap.conf
    $ sudo a2ensite oerworldmap.conf
    $ sudo apache2ctl graceful


Modify the path in `data/permissions/.system`

    AuthUserFile /home/username/oerworldmap/data/auth/htpasswd
    AuthGroupFile /home/username/oerworldmap/data/auth/htgroups

Set up the hostname in `/etc/hosts`

    127.0.0.1	localhost oerworldmap.local


### Create database histories

    $ mkdir -p data/consents/objects
    $ touch data/consents/history
    $ mkdir -p data/commits/objects/
    $ touch data/commits/history


### Setup Play! Application

Download [sbt](http://www.scala-sbt.org/download.html), then

    $ sbt run

### Install UI

UI Components are available at https://github.com/hbz/oerworldmap-ui

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
