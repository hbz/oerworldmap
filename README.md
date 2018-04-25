# Open Educational Resources (OER) World Map

![Travis CI](https://travis-ci.org/hbz/oerworldmap.svg)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=oerworldmap.org)](https://sonarqube.com/dashboard?id=oerworldmap.org)

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

    $ sudo ln -s /home/username/oerworldmap/conf/auth.conf /etc/apache2/sites-available/
    $ sudo a2ensite auth.conf
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

### Work with IDEs

Using [activator](http://www.lightbend.com/community/core-tools/activator-and-sbt), integration to Eclipse and IDEA IntelliJ is provided by running `eclipse` or `idea` from within activator. To run the OER World Map JUnit tests inside IntelliJ, it is necessary to set the test's working directory to the root directory of this project (i. e. `oerworldmap`):

    Run | Edit configurations... | JUnit | <MyTest> | Configuration | Working directory:
    <absolute/path/to/oerworldmap>

## Contribute

### Localizations

You are very welcome to translate the UI of the OER World Map to other languages. To localize the entire site, translations for UI elements as well as for content (such as the [About](https://oerworldmap.org/about) page) are needed. You will create a number of files during the process; if you are not comfortable with using GitHub, you can simply send us those files by email!

#### Localization of UI elements

In order to localize UI elements such as button labels, several files have to be translated: [labels](conf/labels.properties), [descriptions](conf/descriptions.properties) for the input templates and [ui](conf/ui.properties) for the rest of the UI. The format is pretty straight forward, each line consists of a key that is assigned a value:

```
Article.name = Title
Article.description = Teaser
Article.articleBody = Body
Article.dateCreated = Date created
Article.image = Illustrating Image
Article.creator = Creator
```

In order to translate a file, simple copy it and add the target language as a suffix. E.g. to translate the [labels](conf/labels.properties) to German, create a copy of the file named `labels_de.properties`. Then replace all the values by their German translations:

```
Article.name = Titel
Article.description = Aufmacher
Article.articleBody = Text
Article.dateCreated = Erstellungsdatum
Article.image = Illustrierendes Bild
Article.creator = Ersteller
```

Other properties files such as those for country and language names need not be translated manually. We will automatically generate them when deploying your translation.

#### Localization of content

All static pages are located in [public/pages](public/pages). They are formatted using [markdown](https://daringfireball.net/projects/markdown/syntax) along with some structured data at the beginning, the so called front matter:

    ---
    title: FAQ
    ---

    ## What is OER?
    'OER' stands for 'Open Educational Resources' and this
    refers to freely accessible materials that can be used
    for a [range of activities around teaching and learning]
    https://www.opencontent.org/definition/). What makes
    them open is typically an open license instead of a
    traditional copyright license.

In order to translate a static page, copy it and add the target language as a suffix. E.g. to translate the [FAQ](public/pages/FAQ.md) to German, create a copy of the file named `FAQ_de.md`. Then translate the content, including the front matter at the very beginning:

    ---
    title: FAQ
    ---

    ## Was ist OER
    'OER' steht für 'Open Educational Resources' ...

 The title that is specified in the front matter will be automatically added at the top of the page and will also be used in the navigation in the site header.

### Coding conventions

Indent blocks by *two spaces* and wrap lines at *100 characters*. For more
details, refer to the [Google Java Style
Guide](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).

### Bug reports

Please file bugs as an issue labeled "Bug" [here](https://github.com/hbz/oerworldmap/issues/new). Include browser information and screenshot(s) when applicable.

## Attributions

This product includes GeoLite2 data created by MaxMind, available from
<a href="http://www.maxmind.com">http://www.maxmind.com</a>.
