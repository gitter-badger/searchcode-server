<#import "masterTemplate.ftl" as layout />
<@layout.masterTemplate title="Documentation">

<script src="/js/jquery-1.11.1.min.js"></script>
<link rel="stylesheet" href="/css/highlight/default.css">
<link class="codestyle" rel="stylesheet" href="/css/highlight/monokai_sublime.css">
<script src="/js/highlight.pack.js"></script>
<script>hljs.initHighlightingOnLoad();</script>

<div class="row inside-container">

    <div class="col-sm-3">
          <div class="sidebar-module sidebar-module-inset">
            <h2>Documentation</h2>
            <p>How to search and administrate your searchcode server.</p>

          </div>
          <div class="sidebar-module">
            <h5>Guide</h5>
            <ol class="list-unstyled">
              <li><a href="#searching">Searching</a></li>
              <li><a href="#html">HTML Only</a></li>
              <li><a href="#filters">Filters</a></li>
              <li><a href="#owners">Code Owners</li>
              <li><a href="#considerations">Considerations</a></li>
              <li><a href="#estimatedcost">Estimated Cost</a></li>
              <li><a href="#api">API</a></li>
            </ol>
            <h5>Administration</h5>
            <ol class="list-unstyled">
              <li><a href="#web-server">Web Server</a></li>
              <li><a href="#properties">Properties</a></li>
              <li><a href="#settings">Settings</a></li>
              <li><a href="#apikeys">API Keys</a></li>
              <li><a href="#backups">Backups</a></li>
              <li><a href="#recovery">Recovery</a></li>
              <li><a href="#repositories">Repositories</a></li>
              <li><a href="#troubleshooting">Troubleshooting</a></li>
              <li><a href="#support">Support</a></li>
            </ol>
          </div></div>
    <div class="col-sm-9">

      <div>
        <h2>Guide</h2>
        <p>
        With searchcode server you can search across any repository of code that has been added by your administrator.
        </p>
        <p>
        Type in anything you want to find and you will be presented with the results that match with the relevant lines highlighted.
        Searches can filtered down using the right filter panel. Suggested search terms are,
          <ul>
            <li>Function/Method names E.G. <a href="/?q=Format">Format</a>, <a href="/?q=re.compile">re.compile</a></li>
            <li>Constant and variable names E.G. <a href="/?q=ERROR">ERROR</a>, <a href="/?q=username">username</a></li>
            <li>Operations E.G. <a href="/?q=foreach">foreach</a>, <a href="/?q=while">while</a></li>
            <li>Security Flaws E.G. <a href="/?q=eval+%24_GET">eval $_GET</a></li>
            <li>Usage E.G. <a href="/?q=import+flash.display.Sprite%3B">import flash.display.Sprite;</a></li>
            <li>Special Chracters E.G. <a href="/?q=%2B">+</a></li>
          </ul>
        </p>

        <h3 id="searching">Searching</h3>
        <p>
        Type any term you want to search for in the search box and press the enter key. Generally best results can be
        gained by searching for terms that you expect to be close to each other on the same line.
        </p>
        <p>
        The following search operators are supported.

        <dl class="dl-horizontal">
            <dt>AND</dt>
            <dd>Match where documents contain terms on both sides of the operator. E.G. <a href="/?q=test%20AND%20import">test AND import</a></dd>
            <dt>OR</dt>
            <dd>Match where documents contain terms on either side of the operator. E.G. <a href="/?q=test%20OR%20import">test OR import</a></dd>
            <dt>NOT</dt>
            <dd>Match where documents do not contain terms on the right hand side of the operator. E.G. <a href="/?q=test%20NOT%20import">test NOT import</a></dd>
            <dt>( )</dt>
            <dd>Group terms. Allows creation of exclusive matches. E.G. <a href="/?q=(test%20OR%20import)%20AND%20other">(test OR import) AND other</a></dd>
            <dt>*</dt>
            <dd>Wildcard. Only applies at end of a query. E.G. <a href="/?q=test*">test*</a></dd>
        </dl>

        An example using all of the above would be <a href="/?q=(mkdir%20NOT%20sphinx*)%20OR%20(php%20AND%20print*)">(mkdir NOT sphinx*) OR (php AND print*)</a>
        This will search for documents containing mkdir but not starting with sphinx or documents containing php and containing terms starting with print.
        Operators must be in upper case where show or they will be treated as part of the query itself. I.E. to match on documents containing <a href="/?q=and">and</a> search for and lowercase.
        <br /><br />
        Other characters are treated as part of the search itself. This means that a search for something such as <a href="/?q=i%2B%2B%3B">i++;</a> is
        not only a legal search it is likely to return results for most code bases.
        </p>
        <p>
        If a search does not return the results you are expecting or no results at all consider rewriting the query.
        For example searching for <strong>Arrays.asList("a1", "a2", "b1", "c2", "c1")</strong> could be turned into a
        looser query by searching for <strong>Arrays.asList</strong> or <strong>Arrays asList</strong>. Another example would be <strong>EMAIL_ADDRESS_REGEX</strong> for
        <strong>email address regex</strong>.
        </p>
        <p>
        To view the full file that is returned click on the name of the file, or click on any line to be taken to that line.
        Syntax highlighting is enabled for all files less than 1000 lines in length.
        </p>

        <h3 id="html">HTML Only</h3>
        <p>
        You can search using a pure HTML interface (no javascript) <a href="/html/">by clicking here</a>. Note that this page generally
        lags behind the regular interface in functionality.
        </p>

        <h3 id="filters">Filters</h3>
        <p>
        Any search can be filtered down to a specific repository, identified language or code owner using the refinement options.
        Select one or multiple repositories, languages or owners and click the "Filter Selected or "refine search" button to do this.
        </p>
        <p>
        Filters on the normal interface persist between searches. This allows you to select a specific repository or language and continue searching. To clear applied filters uncheck the filters indivudually and click on "Filter Selected". You can also click "Clear Filters" button to clear all active filters. The HTML only page filters are cleared between every new search.
        </p>

        <h3 id="owners">Code Owners</h3>
        <p>
        The owner of any piece of code is determined differently between source control systems. See below for details.
        </p>
        <p>
        GIT owners are determined by counting the number of lines edited by each user. This is then weighted
        against the last commit time. For example, Bob added a file of 100 lines in length 1 year ago.
        Mary modified 30 lines of the file last week. In this situation Mary would be marked as the owner as she has modified
        enough of the file and recently enough to be more famililar with it then Bob would be. If she has only modified a single
        line however Bob would still be marked as the owner.
        </p>
        <p>
        The name is taken based on the git config user.name setting attached to the user in commits.
        </p>
        <p>
        SVN owners are determined by looking at the last user to change the file. For example, Bob edited a single line in a file with 100 lines. Bob will be
        considered the owner even if Mary edited the other 99 previously.
        </p>


        <h3 id="considerations">Considerations</h3>
        <p>Source code is complex to search. As such the following restrictions currently apply
        <ul>
            <li>Relevant lines in the search display favor lines in the beginning of file however there may be other matching lines within the file. By default searchcode will only inspect the first 10000 lines for matches when serving results.</li>
            <li>The following characters are not indexed and will be ignored from searches <strong>< > ) ( [ ] | =</strong>.</li>
            <li>Where possible if there are no matches searchcode server will attempt to suggest an alternate search which is more likely to produce results.</li>
        </ul>
        </p>

        <h3 id="estimatedcost">Estimated Cost</h3>
        <p>The estimated cost for any file or project is created using the <a target="_blank" href="https://en.wikipedia.org/wiki/COCOMO">Basic COCOMO</a>
        algorithmic software cost estimation model. The cost reflected includes design, coding, testing, documentation for both
        developers and users, equipment, buildings etc... which can result in a higher estimate then would be expected. Generally
        consider this the cost of developing the code, and not what it is "worth".
        It is based on an average salary of $56,000 per year but this value can be changed by the system administrator if
        the values appear to be too out of expectation.</p>


        <h3 id="api">API</h3>
        <p>API endpoints offered by your searchcode server instance are described below. Note that some require API authentication
        which will also be covered.</p>
        <p>
          <h4>API Authentication</h4>
          API authentication is done through the use of shared secret key HMAC generation. If enabled you will be required to sign
          the arguments sent to the API endpoint as detailed. Ask your administrator for a public and private key to be generated for you
          if you require access to the API.
          <br><br>
          To sign a request see the below example in Python demonstrating how to perform all repository API calls.
          The most important thing to note is that parameter order is important. All API endpoints will list the order
          that parameters should have passed in.
          <br><br>
<textarea style="font-family: monospace,serif; width:100%; height:150px;" disabled="true">from hashlib import sha1
from hmac import new as hmac
import urllib2
import json
import urllib
import pprint

publickey = "REALPUBLICKEYHERE"
privatekey = "REALPRIVATEKEYHERE"

reponame = "myrepo"
repourl = "myrepourl"
repotype = "git"
repousername = ""
repopassword = ""
reposource = ""
repobranch = "master"

message = "pub=%s&reponame=%s&repourl=%s&repotype=%s&repousername=%s&repopassword=%s&reposource=%s&repobranch=%s" % (
        urllib.quote_plus(publickey),
        urllib.quote_plus(reponame),
        urllib.quote_plus(repourl),
        urllib.quote_plus(repotype),
        urllib.quote_plus(repousername),
        urllib.quote_plus(repopassword),
        urllib.quote_plus(reposource),
        urllib.quote_plus(repobranch)
    )

sig = hmac(privatekey, message, sha1).hexdigest()

url = "http://localhost:8080/api/repo/add/?sig=%s&%s" % (urllib.quote_plus(sig), message)

data = urllib2.urlopen(url)
data = data.read()

data = json.loads(data)
print data['sucessful'], data['message']

################################################################

message = "pub=%s" % (urllib.quote_plus(publickey))

sig = hmac(privatekey, message, sha1).hexdigest()

url = "http://localhost:8080/api/repo/list/?sig=%s&%s" % (urllib.quote_plus(sig), message)

data = urllib2.urlopen(url)
data = data.read()

data = json.loads(data)
print data['sucessful'], data['message'], data['repoResultList']

################################################################

message = "pub=%s&reponame=%s" % (
        urllib.quote_plus(publickey),
        urllib.quote_plus(reponame),
    )

sig = hmac(privatekey, message, sha1).hexdigest()

url = "http://localhost:8080/api/repo/delete/?sig=%s&%s" % (urllib.quote_plus(sig), message)

data = urllib2.urlopen(url)
data = data.read()

data = json.loads(data)
print data['sucessful'], data['message']</textarea>
          <br><br>
          To achive the same result in Java use <a href="https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/digest/HmacUtils.html">HmacUtils</a> as follows,
          <br><br>
          <textarea style="font-family: monospace,serif; width:100%; height:30px;" disabled="true">String myHmac = HmacUtils.hmacSha1Hex(MYPRIVATEKEY, PARAMSTOHMAC);</textarea>

        </p>
        <p>
          <h4>Repository API (secured)</h4>
          The repository API allows you to list, add/update and delete repositories that are currently being indexed within your searchcode server instance. All calls to the repository API methods are secured if the appropiate property has been set by your administrator.
        
          <h5>Endpoint List All Repositories</h5>
          <pre>/api/repo/list/</pre>
          <p>Some repositories returned by this endpoint may be queued for deletion. They will continue to appear in this list until they
          are sucessfully removed.</p>
          <h5>Params</h5>
              <ul>
                <li>sig: signed value (optional if unsecured)</li>
                <li>pub: the public key supplied by your administrator (optional if unsecured)</li>
              </ul>
        
          <h5>Signing</h5>
          To sign requests to this endpoint you need to HMAC as follows<br>
          <pre>hmac_sha1("MYPRIVATEKEY", "pub=MYPUBLICKEY")</pre>

          <h5>Examples</h5>
          <pre>http://localhost/api/repo/list/</pre>
          <pre>http://localhost/api/repo/list/?sig=SIGNEDKEY&pub=PUBLICKEY</pre>
        
          <h5>Return Field Definitions</h5>

          <dl class="dl-horizontal">
            <dt>message</dt>
            <dd>A message containing debug information if the request fails.</dd>
            <dt>sucessful</dt>
            <dd>True or false value if the request was processed.</dd>

            <dt>repoResultList</dt>
            <dd>An array containing the repository results. Will only be present if the call was sucessful.
              <dl class="dl-horizontal">
                <dt>branch</dt>
                <dd>Branch that is being monitored. N.B. this is not applicable to SVN repositories.</dd>
                <dt>name</dt>
                <dd>name used to idenity this repository.</dd>
                <dt>password</dt>
                <dd>The password used to authenticate for clone and update requests</dd>
                <dt>rowId</dt>
                <dd>Only used internally. Refers to the rowId of the database</dd>
                <dt>scm</dt>
                <dd>The source control management system used</dd>
                <dt>source</dt>
                <dd>The source URL that should point to where this repository is located</dd>
                <dt>url</dt>
                <dd>The endpoint URL that us used for clone and update requests</dd>
                <dt>username</dt>
                <dd>The username used to authenticate for clone and update requests</dd>
              </dl>
            </dd>
          </dl>

          <h5>Sample Response</h5>
          <pre>{
    "message": "",
    "repoResultList": [
        {
            "branch": "master",
            "name": "test",
            "password": "",
            "rowId": 1,
            "scm": "git",
            "source": "http://github.com/myuser/myrepo",
            "url": "git://github.com/myuser/myrepo.git",
            "username": ""
        }
    ],
    "sucessful": true
}</pre>


        <h5>Endpoint Add Repository</h5>
          <pre>/api/repo/add/</pre>
          <p>It is not possible to update an existing repository. To do so you must first delete the existing repository and wait for the background
           tasks finish cleaning the repository.
          </p>
          <h5>Params</h5>
            <ul>
              <li>sig: signed value (optional if unsecured)</li>
              <li>pub: the public key supplied by your administrator (optional if unsecured)</li>

              <li>reponame: unique name to identify the repository if matches existing it will delete the existing and recreate</li>
              <li>repourl: the url to the repository endpoint</li>
              <li>repotype: the type of repository this is, NB only git is currently supported</li>
              <li>repousername: username used to pull from the repository</li>
              <li>repopassword: password used to pull from the repository</li>
              <li>reposource: a http link pointing where the repository can be browsed or a helpful link</li>
              <li>repobranch: what branch should be indexed</li>
            </ul>

        
          <h5>Signing</h5>
          To sign requests to this endpoint you need to HMAC as follows<br>
          <pre>hmac_sha1("MYPRIVATEKEY", "pub=MYPUBLICKEY&reponame=REPONAME&repourl=REPOURL&repotype=REPOTYPE&repousername=REPOUSERNAME&repopassword=REPOPASSWORD&reposource=REPOSOURCE&repobranch=REPOBRANCH")</pre>

          <h5>Examples</h5>
          <pre>http://localhost/api/repo/add/?reponame=testing&repourl=git://github.com/test/test.git&repotype=git&repousername=MYUSER&repopassword=MYPASSWORD&reposource=http://githib.com/test/test/&repobranch=master</pre>
          <pre>http://localhost/api/repo/add/?sig=SIGNEDKEY&pub=PUBLICKEY&reponame=testing&repourl=git://github.com/test/test.git&repotype=git&repousername=MYUSER&repopassword=MYPASSWORD&reposource=http://githib.com/test/test/&repobranch=master</pre>
        
          <h5>Return Field Definitions</h5>

          <dl class="dl-horizontal">
            <dt>message</dt>
            <dd>A message containing debug information if the request fails.</dd>
            <dt>sucessful</dt>
            <dd>True or false value if the request was processed.</dd>
          </dl>

          <h5>Sample Response</h5>
          <pre>{
    "message": "added repository sucessfully",
    "sucessful": true
}</pre>


        <h5>Endpoint Delete Repository</h5>
          <pre>/api/repo/delete/</pre>
          <p>Successful calls to this endpoint will insert a request into a queue to remove the repository. The actual deletion
          can take several minutes.<p>
          <h5>Params</h5>
            <ul>
              <li>sig: signed value (optional if unsecured)</li>
              <li>pub: the public key supplied by your administrator (optional if unsecured)</li>
              <li>reponame: unique name to identify the repository</li>
            </ul>
        
          <h5>Signing</h5>
          To sign requests to this endpoint you need to HMAC as follows<br>
          <pre>hmac_sha1("MYPRIVATEKEY", "pub=MYPUBLICKEY&reponame=REPONAME)"</pre>

          <h5>Examples</h5>
          <pre>http://localhost/api/repo/delete/?reponame=testing</pre>
          <pre>http://localhost/api/repo/delete/?sig=SIGNEDKEY&pub=PUBLICKEY&reponame=testing</pre>
        
          <h5>Return Field Definitions</h5>

          <dl class="dl-horizontal">
            <dt>message</dt>
            <dd>A message containing debug information if the request fails.</dd>
            <dt>sucessful</dt>
            <dd>True or false value if the request was processed.</dd>
          </dl>

          <h5>Sample Response</h5>
          <pre>{
    "message": "deleted repository sucessfully",
    "sucessful": true
}</pre>


        <!--<p>

        <h4>Code Search API</h4>
        <pre><code>{
    "totalHits": 1,
    "page": 0,
    "query": "test",
    "altQuery": ["tests", "testing"],
    "codeResultList": [{
        "matchingResults": [{
            "line": "",
            "matching": false,
            "addBreak": false,
            "lineMatches": 0,
            "lineNumber": 10
        }, {
            "line": "import test",
            "matching": true,
            "addBreak": false,
            "lineMatches": 1,
            "lineNumber": 11
        }, {
            "line": "# mycomment",
            "matching": false,
            "addBreak": false,
            "lineMatches": 0,
            "lineNumber": 12
        }],
        "codePath": "my-cool-repository/tests/test_config.py",
        "fileName": "test_config.py",
        "fileLocation": "",
        "md5hash": "3d08d3e0286eb5fc6c65316fafdd7ef7",
        "languageName": "Python",
        "codeLines": "31",
        "documentId": 212,
        "repoName": "my-cool-repository",
        "repoLocation": "https://github.com/my-cool-repository/my-cool-repository.git",
        "codeOwner": "Bob Secret"
    }],
    "pages": [0, 1],
    "languageFacetResults": [{
        "languageName": "Python",
        "count": 5,
        "selected": false
    }, {
        "languageName": "Java",
        "count": 3,
        "selected": false
    }],
    "repoFacetResults": [{
        "repoName": "my-cool-repository",
        "count": 5,
        "selected": false
    }, {
        "repoName": "test",
        "count": 3,
        "selected": false
    }]
    "repoOwnerResults": [{
        "owner": "Bob Secret",
        "count": 5,
        "selected": false
    }, {
        "owner": "Alice Secret",
        "count": 3,
        "selected": false
    }]
}</code></pre>
        <p>-->
      </div>

      <hr>

      <div>
        <h2>Administration</h2>
        <p>
        searchcode server is designed to require as little maintenance as possible and look after itself once setup and
        repositories are indexed. However it can be tuned using the settings mentioned below in the searchcode.properties
        file or through the <a href="/admin/settings/">admin settings page</a>.
        </p>

        <h3 id="web-server">Web Server</h3>

        <p>searchcode server uses the high performance jetty web server. It should perform well even with thousands of requests
        as a front facing solution. If a reverse proxy solution is required there is no need to configure static assets, simply
        configure all requests to pass back to searchcode server on 127.0.0.1. You should also set the property only_localhost to true
        in this case.</p>

        <h3 id="properties">Properties</h3>

        <p>
        The searchcode.properties file in the base directory is a simple text file that can be used to configure aspects of searchcode server. By default
        it is setup using suggested defaults. <b>It is important to note that the password to administer your server is located
        in this file</b>.
        To apply changes, modify the file as required then restart searchcode. All slashes used in the properties file should be forward not backwards. I.E. Unix style not Windows.

            <dl class="dl-horizontal">
              <dt id="password">password</dt>
              <dd>The password used to login to the admin section. <strong>It is suggested that this is changed.</strong></dd>
              <dt>database</dt>
              <dd>Do not modify this value. Additional database support is planned but not implemented.</dd>
              <dt>sqlite_file</dt>
              <dd>The name of the sqlite database file. If you change this you will need to copy or move the existing file to match the new value.</dd>
              <dt>server_port</dt>
              <dd>The port number that will be bound to. Needs to be a number or will default to 8080.</dd>
              <dt>repository_location</dt>
              <dd>Path to where the checked out repositories will be.</dd>
              <dt>index_location</dt>
              <dd>Path to where the index will be built.</dd>
              <dt>facets_location</dt>
              <dd>Path to where the index facets will be built. This must not be the same value as index_location.</dd>
              <dt>check_repo_chages</dt>
              <dd>Interval in seconds to check when repositories will be scanned for changes. Needs to be a number or will default to 600.</dd>
              <dt>only_localhost</dt>
              <dd>Boolean value true or false. Will only process connections on 127.0.0.1 (not localhost) if set to true and return 204 content not found otherwise. By default set to false.</dd>
              <dt>low_memory</dt>
              <dd>If running searchcode server on a low memory system set this to true. It will use less memory at the expense of indexing time. If set to false you may experience out of memory exceptions if you attempt to index large repositories with insufficient RAM. By default set to false.</dd>
              <dt>spelling_corrector_size</dt>
              <dd>Number of most common "words" to keep for when spell suggesting. When on a memory constrained system it can be advisable to reduce the size. Needs to be a number or will default to 10000.</dd>
              <dt>max_document_queue_size</dt>
              <dd>Maximum number of documents to store in indexing queue. When on a memory constrained system it can be advisable to reduce the size. Needs to be a number or will default to 1000.</dd>
              <dt>max_document_queue_line_size</dt>
              <dd>Maximum number of lines of code to store in indexing queue. This is a soft cap which can be exceeded to allow large documents to be indexed. When on a memory constrained system it can be advisable to reduce the size. 100000 lines equals about 200mb of in memory storage which will be used during the index pipeline. Needs to be a number or will default to 100000.</dd>
              <dt>max_file_line_depth</dt>
              <dd>Maximum number of lines in a file to index. If you want to index very large files set this value to a high number and lower the size of max_document_queue_size to avoid out of memory exceptions. 100000 lines equals about 200mb of in memory storage which will be used during the index pipeline. Needs to be a number or will default to 10000.</dd>
              <dt>use_system_git</dt>
              <dd>If you have git installed on the system you can choose to use external calls to it. This may resolve memory pressure issues but will generally be slower. By default set to false.</dd>
              <dt>git_binary_path</dt>
              <dd>If you enable use_system_git you need to ensure that this equals the path to your git executable for your system. By default set to /usr/bin/git</dd>
              <dt>log_level</dt>
              <dd>What level of logging is requested both to STDOUT and the default log file. Accepts the uppercase values of INFO, WARNING, SEVERE or OFF. By default set to SEVERE. Critical warnings will still be printed to STDOUT.</dd>
              <dt>api_enabled</dt>
              <dd>Boolean value true or false. Should the searchcode server API be enabled. By default set to false.</dd>
              <dt>api_key_authentication</dt>
              <dd>Boolean value true or false. Should the searchcode server API be secured through the use of manually created API keys. If you expose searchcode server publicly and enable the API you should set this to true. By default set to true.</dd>
              <dt>svn_enabled</dt>
              <dd>Boolean value true or false. Will SVN repositories added be crawled and indexed. If you set this value be sure to set svn_binary_path as well. By default set to false.</dd>
              <dt>svn_binary_path</dt>
              <dd>If svn_enabled is set to true you need to ensure that this equals the path to your svn executable for your system. By default set to /usr/bin/svn</dd>
            </dl>

        </p>

        <h3 id="settings">Settings</h3>

        <p>
        The admin settings page can be used change look and feel settings for searchcode server. Change the settings
        on the page. Changes are applied instantly.
        <#if isCommunity??>
                    <#if isCommunity == true>
                    You are using the community edition of searchcode server. As such you will be unable to change anything here. If you would like the ability to configure the settings page
                    you can purchase a copy at <a href="https://searchcode.com/product/download/">https://searchcode.com/product/download/</a>
                    </#if>
        </#if>

          <dl class="dl-horizontal">
            <dt>Logo</dt>
            <dd>The logo that appears on the top left of all searchcode server pages. Should be added as a Base64 encoded image string.</dd>
            <dt>Syntax Highlighter</dt>
            <dd>Change the highlight style for code result pages.</dd>
            <dt>Average Salary</dt>
            <dd>Used as the base salary for the code display calculation. See <a href="#estimatedcost">estimated cost</a> for more
            details about this value.</dd>
            <dt>Match Lines</dt>
            <dd>Maximum number of lines to find for a search. Increasing this value will display more on search result pages for a given match. Needs to be a number or will default to 15.</dd>
            <dt>Max Line Depth</dt>
            <dd>How many lines into a file should be scanned for matching code. Increasing this value will slow down searches for larger files but is more likely to display the correct lines. Needs to be a number or will default to 10000.</dd>
            <dt>Minified Length</dt>
            <dd>What the average length of lines in a file (ignoring empty) needs to be to mark the file as minified and being excluded from being indexed. Changing this value will affect files as they are re-indexed when the watched repositories change. Needs to be a number or will default to 255.</dd>
          </dl>
        </p>

        <h3 id="apikeys">API Keys</h3>

        <p>
        The api key page is used to maintain keys used for authenticated API requests. This page is only relevant if you firstly
        enble the API through properties and then enable authenticated API reqeusts as well. To generate a key click the "Generate New API Key"
        button. A new API key will be created and appear at the bottom of the list. The key consists of two parts. The first portion is the public
        key which is used to identify who is making the request to the API. The second is the private key and should be shared only with the consuming
        application. This key is used to sign the request. To delete a key click the delete button next to the key you wish to remove. Generally it is
        considered good practice to create individual keys for each application using the API.
        </p>

        <h3 id="backups">Backups</h3>
        <p>Generally searchcode server should only need the <b>searchcode.properties</b> and <b>searchcode.sqlite</b> files to be backed up.
        However where many repositories are indexed or when connectivity to source control can be problematic you may want to back up
        the index and repo directories and their contents.</p>

        <h3 id="recovery">Recovery / Restore</h3>
        <p>Assuming you want to recover searchcode you will need to install the application sources. Then copy a backup of the
        <b>searchcode.sqlite</b> and <b>searchcode.properties</b> files into the same directory. When started searchcode will
        analyse the code and rebuild the index. This process will take longer for setups that contain many or large repositories.
        If faster restores are required restore the index and repo directories as well.</p>

        <h3 id="repositories">Repositories</h3>
        <p>
        To index a repository browse to the <a href="/admin/">admin</a> page. Enter a repository name and url for publicly
        available repositories and for private a username and password for a user with enough access to checkout a copy
        of the repository. Repo Source should be a URL that relates to the repository (but can be anything) and will appear
        as a link on the code pages.
        When done click "Add Repo". The repository will be downloaded and indexed as soon as any other
        indexing operations are finished. Note that repository names cannot include a space, and any spaces will be replaced
        with a hyphen character.
        </p>
        <p>
        Both GIT and SVN repositories are able to be indexed. To enable indexing of SVN repositories set the property value
        svn_enabled to true and svn_binary_path to the path of your SVN executable.
        </p>
        <p>
        To delete a repository click the delete button at the end of the repository list. This will remove all copies of code
        from disk and the index. This action is not reversible. To undo the operation add the repository again. Note that
        all delete operations are queued and it may take several minutes for the repository to be removed.
        </p>
        <p>
        Updating the details of a repository will require you to delete the repository and add it again with the new details.
        </p>

        <p>To quickly add a large amount of repositories use the <a href="/admin/bulk/">bulk admin</a> page. This page will only
        allow the adding of repositories using a CSV format with one repository per line.
        </p>
        <p>
        The format for adding follows.<br><br>
        <code>reponame,scm,gitrepolocation,username,password,repourl</code><br><br>

        For example a public repository which does not require username or password<br><br>

        <code>phindex,git,https://github.com/boyter/Phindex.git,,https://github.com/boyter/Phindex</code> <small>*</small><br><br>

        For example a private repository which requires a username and password<br><br>

        <code>searchcode,git,https://searchcode@bitbucket.org/searchcode/hosting.git,myusername,mypassword,</code><br><br>

        Note that the trailing comma is required.<br><br>

        <small>* This is a real repository can can be indexed. Copy paste into the bulk admin page to test.</small>
        </p>

        <h3 id="troubleshooting">Troubleshooting</h3>
        <p>
          <b>A repository is not being indexed?</b><br/>
          Check the console output, you should see something similar to<br />
          <pre>ERROR - caught a class org.eclipse.jgit.api.errors.TransportException with message: https://username@bitbucket.org/username/myrepo.git: not authorized</pre><br />
          This means your username or password for the repository is invalid. Try pulling a copy down locally and replacing the credentials.
          </p>
        <p>
          <b>A file in a repository is not being indexed?</b><br/>
          Files with an average file line length >= 255 are considered minified and will not be indexed. You should get a message like the below on the console saying as such when trying to index the file.<br />
          <pre>Appears to be minified will not index FILENAME</pre>
        </p>
        <p>
          <b>A repository is not being indexed on Windows</b><br/>
          There are reserved file names on Windows such as CON, PRN, AUX, NUL, COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9, LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9.
          If a repository created on another OS contains one of these filenames it is likely that the attempt to clone or checkout will fail. Generally
          it is better to deploy searchcode server on a Unix style OS to avoid this problem. If you are only going to index repositories that
          were created using Windows then Windows is still a valid choice.
        </p>
        <p>
          <b>OutOfMemoryError</b><br/>
          If you are are getting the classic java out of memory error such as <br />
          <pre>java.lang.OutOfMemoryError: Java heap space</pre><br />
          There are a few things you can do. Try each one individually with a restart of your searchcode server instance.

          <ol>
            <li>Upgrade the amount of system RAM available on the host system.</li>
            <li>Edit the searchcode-server.sh or searchcode-server.bat file and add the Xmx and Xms arguments.</li>
            <li>Install git and set the searchcode.properties property git_binary_path to the path of your git binary and set use_system_git to true.</li>
            <li>Lower the value of max_document_queue_size.</li>
            <li>Lower the value of max_file_line_depth to be less than the expected length of any file you need to search.</li>
            <li>Set the searchcode.properties property low_memory to true and restart your instance. This should be method of last resort as it will lower memory usage with the impact of less indexing performance.</li>
            <li>Set the searchcode.properties property spelling_corrector_size to a lower number such as 1000.</li>
          </ol>
        </p>

        <p>
          <b>java.io.IOException: Too many open files</b><br/>
          This issue typically occurs on Unix/Linux servers with a low ulimit.
          If you are getting errors like the above you may need to change your ulimit to a higher number as the default
          of 1024 for most systems can be too low.<br />
        </p>
        <p>
          <b>Help! Nothing is working!</b><br/>
          Its possible that you may enter a state where nothing is working. In this case save the console output and try
          restarting searchcode. This may resolve the issue. If not, try stopping searchcode and deleting the index and repo directories.
          This will force searchcode server to re-download and re-index. If all else fails contact support.
        </p>

        <h3 id="support">Support</h3>

        <#if isCommunity??>
            <#if isCommunity == true>
            <p>
                You are using the community edition of searchcode server. Sorry but you are own your own. If you would like support (and the ability to configure the settings page)
                you can purchase a copy at <a href="https://searchcode.com/product/download/">https://searchcode.com/product/download/</a>
            </p>
            <#else>
            <p>
            To get support for your searchcode server instance email Ben directly at searchcode@boyter.org Please include the following
            information along with the problem you are experiencing.
            <ul>
              <li>Operating system used</li>
              <li>Number of repositories indexed</li>
              <li>Approx size of repositories when checked out</li>
              <li>Java version</li>
              <li>Details from the console output</li>
              <li>Hardware specifications</li>
            </ul>
            </p>
            </#if>
        </#if>


</div> <!-- end row -->

</@layout.masterTemplate>