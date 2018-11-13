**[Dev Tools](#necessary-tools)** | **[Maven Access](#access-to-the-polaris-maven-private-repo)** | **[Deploy](#deployment)** | **[Messaging](#messaging)** | **[Storage](#storage)** | **[Finance](#finance)**

# Cap'n Hook - Callbacks Made Simple

<p align="center">
  <img src="doc/img/hook.jpg" width="400" height="357" border="0" />
</p>

The _web callback_ has become q very popular way for applications to notify
clients that some event happened without the client constantly polling for
any status updates. The problem with _adding_ callbacks to a service is
that there is nothing technically challenging about doing them, but getting
them _right_ is annoying and takes a lot of time.

* Do you handle retries if the client isn't responding?
* Do you allow the client to indicate the type of call? (GET, POST, etc.)
* Is there a simple asynchronous queue for sending these out?

There are a lot of little details that can make a big difference in the
overall utility of the callbacks you send.

_Cap'n Hook_ is all about making this as simple as possible.

## Usage

Using _Cap'n Hook_ is really a two-part process: you have to have some
_registration_ of the call-backs to make, and then you have to have a function
to fire off a call-back. Since call-backs are only really useful if they
contain data, the firing of a call-back really needs to have two things: the
identifying name of the call-back to make, and the data to send along with
it.

In order to register a new...

## Development

### Necessary Tools

**[Homebrew](http://brew.sh/)** - all of the following tools can be installed with Homebrew. If it's not already installed on you laptop, it's easy enough to go to the website, run the command to install it, and then continue to the next step. If you are on linux, expect to be installing some scripts and RPMs, but everything is available there as well - just not as convenient.

**[JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)** - it might be nice to get the JDK 1.6 from Apple, but it's _essential_ to get the JDK 1.8 from Oracle. This is a download and package install, but it's pretty simple to do and sets up it's own updater for future releases.

**[AWS CLI](https://aws.amazon.com/cli/)** - this provides a command line interface for
interacting with AWS resources. This is required to push project documentation to S3 via
`make docs`.
```bash
$ brew install awscli
```

***If you're on High Sierra, /usr/local can no longer be chown'd. You can use:***
```base
$ sudo chown -R $(whoami) $(brew --prefix)/*
```

**[AWS EB CLI](http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3.html)** -
these are the command line tools for dealing with Amazon's Elasticbeanstalk and
that's how the Docker instances are being deployed. These tools help monitor and
manage the Docker instances at AWS. Simply install from Homebrew:
```bash
$ brew install awsebcli
```

**[Leiningen](http://leiningen.org/)** - it is the _swiss army knife_ of clojure development - library version control, build, test, run, deploy tool all in one with the ability to create extensions. On the Mac, this is installed very easily with:
```bash
$ brew install leiningen
```
and on linux you can download the [lein script](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein) to _somewhere_ in your `$PATH`, and make it executable. Then:
```bash
$ lein
```
and it'll download everything it needs to run properly.

**[Postgres](http://www.postgresql.org/)** - at the current time, this is the back-end persistence and it's really a very simple product to install and use. On the Mac, simply:
```bash
$ brew install postgresql
```
and when it's done, follow the directions to run it - the configuration is fine as it is. Using `brew services`:
```bash
$ brew services start postgresql
```
and it's running everytime you log in.

**[Redis](http://redis.io/)** - at the current time, this is the caching layer
that saves a lot of the load on the back-end persistence and it's really a very
simple product to install and use. On the Mac, simply:
```bash
$ brew install redis
```
and when it's done, follow the directions to run it - the configuration is fine as it is. Using `brew services`:
```bash
$ brew services start redis
```
and it's running everytime you log in.

### Access to the `/polaris-maven/` Private Repo

At the current time, we don't have Artifactory or Maven running in the data center,
but the Maven functionality is being picked up by the [Leiningen](http://leiningen.org/) plugin - [s3-wagon-private](https://github.com/s3-wagon-private/s3-wagon-private). This is represented by the inclusion of the lines in the `project.clj`:
```clojure
  :plugins [[s3-wagon-private/s3-wagon-private "1.3.0"]]
```
where the latter is the important plugin, and then the declaration of the `:repositories`:
```clojure
  :repositories [["releases" {:url "s3p://polaris-maven/releases/"
                              :username [:gpg :env/polaris_username]
                              :passphrase [:gpg :env/polaris_passphrase]
                              :sign-releases false}]
                 ["snapshots" {:url "s3p://polaris-maven/snapshots/"
                               :username [:gpg :env/polaris_username]
                               :passphrase [:gpg :env/polaris_passphrase]}]]
```

There is already the S3 bucket `/polaris-maven/` created for the organization, and
within that, the `releases` and `snapshots` folders. The only thing each developer
has to do is to set up their credentials for accessing this S3 bucket.

This can be done two ways - via GPG and the environment.

#### Getting your AWS Access Key ID and Secret Access Key

The credentials that you'll need to have are the AWS _Access Key ID_ and
_Secret Access Key_. This isn't all that hard, and is described [here](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html).
Basically, You get to the IAM Console - after logging in and then:

* click **Users**
* select your username
* click **User Actions**, and then **Manage Access Keys**
* click **Create Access Key** and then write down the credentials and optionall
  **Download Credentials** to save them to your machine

At this point, you have the credentials you need to put them into one of the two
schemes that Leiningen allows.

#### Using GPG to Store your Credentials

If you already have GPG on your machine, you can use it to encrypt the
`~/.lein/credentials.clj` file that contains the credentials you obtained
in the previous step. This is detailed in the [Leiningen Deployment](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md) docs, and while it's not
_easy_ it is a possibility for those that feel security is paramount.

Leiningen also has a helpful [doc](https://github.com/technomancy/leiningen/blob/stable/doc/GPG.md) on the use of GPG, to get folks started.

#### Using the Environment to Store your Credentials

_**By far**_ the easiest way to provide your credentials is to place them in two
environment variables:
```bash
$ export POLARIS_USERNAME='<AWS_Access_Key_ID>'
$ export POLARIS_PASSPHRASE='<AWS_Secret_Access_Key>'
```
and these can be placed in your `~/.bashrc`, or some other script that you run.

Once these credentials are available to Leiningen, you will be able to:
```bash
$ lein deps
```
and pull down the latest version(s) of the resources that are located in the
`/polaris-maven/` repo in S3.

#### Running the Tests

There are many tests that are in the repo, and to run them all takes a few
minutes. So we have broken them up to be "All but the massive APR Tests", and
everything else - including some critical 50-ish APR tests.

The default is to run everything but the massive APR tests:
```bash
$ lein test
```
and you get answers in a few seconds.

To run the 8,000+ APR tests, you would run:
```bash
$ lein test :apr
```
and to run _everything_ you would run:
```bash
$ lein test :all
```

## Deployment

The deployment of this library to the private `/polaris-maven/` S3 repo is just
the same as all other Leiningen projects. Simply:
```bash
$ lein deploy
```
and a new uberjar will be built and the version in the `project.clj` will be used
to name the jar and then it'll get sent to S3. Simple.

But the real process of deployment is in what you do **before** that step.

### Update the Version Number in `project.clj`

Each deployment will require a new version number, and the rules are very simple:

* if **all** changes are Bug Fixes, then increment the _smallest_ value in the version.
* if **any** change made _**breaks**_ the external API, then increment the _largest_ value in the version.
* if it's anything else - increment the _middle_ number in the version.

These are simple rules. If you did _more_ than Bug Fixes, but broke no external APIs, then it's a _middle_ version number fix. If you broke existing APIs, then it's a _large_ number change - even if you added other features and fixed bugs.

The version number must change, and the release notes should be updated as well. But
there is no room for discussion on what should change - the rules are very clear.

### Tag the Git Repo with the New Version

Once you have updated the version in the `project.clj` file, you need to tag the git repo with the version. This is simple:
```bash
$ git tag -a v1.2.1
```
and supply a concise, reasonable, sentence for this version tag. Please note the `v`
on the front of the tag. This will make things more uniform for all version tags.

You can then push the tag with:
```bash
$ git push --tags
```

### Finally, Deploy the Library to Maven

At this point, you can do the deploy:
```bash
$ lein deploy
```
and you should be good to go.

## Messaging
```ns-toolkit``` makes use of Amazon's [SNS](https://aws.amazon.com/sns/) and [SQS](https://aws.amazon.com/sqs/) to produce messages and allow for multiple consumers. Functions in the ns-toolkit.messaging namespace handle publishing, polling, and deleting messages.

### Listening for Messages

To listen for messages being published to a topic, call the `poll!` function passing a string representing the topic and subscriber as an argument. Note, there is an important naming convention here: the argument must be the topic name, followed by an underscore, followed by the subscriber name.

```clojure
(poll! "save-loan-prod_my-subscriber")
```

This will automatically create an [SQS](https://aws.amazon.com/sqs/) queue named `save-loan-prod_my-subscriber` if it does not already exist and subscribe it to the `save-loan-prod` topic. By default, `poll!` will retrieve 10 messages. The number of messages to retrieve can optionally be passed as an argument:

```clojure
;; Retrieve 30 messages
[qn & [n to vis]]
(poll! "save-loan-prod_my-subscriber" 30)
;; Retrieve 30 messages within 10 seconds
(poll! "save-loan-prod_my-subscriber" 30 10000)
;; Retrieve 30 messages within 10 seconds, setting the visibility timeout of each message to 5 seconds
;; Visibility timeout is the amount of time an application has to process a message before it's returned to the queue, defaulted to 20 seconds
(poll! "save-loan-prod_my-subscriber" 30 10000 5000)
;; If a message is processed successfully, it should be deleted afterwards
(let [msg (poll! "save-loan-prod_my-subscriber" 1)]
  ;; process the msg, then delete
  (delete! msg))
```

If using ns-toolkit version 0.25.0 or below, creating SNS topics and SQS queues and wiring the two together is a manual process that must be completed via the AWS console as a prerequisite to using the messaging functions. Instructions follow explaining how to do so. As of ns-toolkit v0.26.0, the SQS queue is created automatically and subscribed to the topic, so it's only necessary to follow Step 1 of the instructions.

The use case for using SNS and SQS together is to allow for one message to be published at once to multiple consumers. Each consumer would be listening on its own SQS queue.

Amazon has detailed documentation on setting up sending SNS messages to SQS queues. However, the following steps should be enough to get up and running.

#### Step 1: Create an SNS topic
1.1. Navigate to SNS via the AWS console
![SNS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sns-menu.png)

1.2. Select the `Topics` menu item on the left and click `Create new topic`
![Create Topic](https://s3.amazonaws.com/ns-toolkit-docs/create-topic.png)

1.3. Enter a `topic name` and click `create topic`
![Create Topic](https://s3.amazonaws.com/ns-toolkit-docs/create-topic-modal.png)

#### Step 2: Create an SQS queue
2.1. Navigate to SQS via the AWS console
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sqs-menu.png)

2.2. Click ```Create New Queue``` and enter a name for the queue. The defaults are reasonable and can be changed here.
![Create Topic](https://s3.amazonaws.com/ns-toolkit-docs/create-new-queue.png)

2.3. Filter the queue list by entering the queue name into the `Filter by Prefix` field and copy the queue's `ARN` from the `Details` tab.
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/queue-arn.png)

#### Step 3: Subscribe the SQS queue to the SNS topic

3.1. Navigate back to the SNS console Topics list and select the ```topic``` created in step 1.3 Under the ```Actions``` menu, choose ```Subscribe to topic```.
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sns-actions-subscribe.png)

3.2. For ```Protocol```, select ```Amazon SQS```. For ```Endpoint```, paste in the ```ARN``` selected in step 2.3.
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sns-create-sub.png)

#### Step 4: Grant the SNS topic to send messages to the SQS queue

4.1. Copy the topic's ```ARN``` and navigate back to the ```SQS``` console

4.2. Select the queue created in step 2.2

4.3. Via the ```Permissions``` menu, select ```Add Permission```
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sqs-permissions-tab.png)

4.4. Leave ```Effect``` as ```Allow```

4.5. Check the checkbox next to ```Everybody```

4.6. Under the ```Actions``` dropdown, select ```SendMessage```
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sqs-permission-send.png)

4.7. Before saving, click ```Add Conditions```

4.8. For ```Condition``` choose ```ArnEquals```

4.9. For ```Key``` choose ```aws:SourceArn```

4.10. For ```Value``` paste in the topic ```ARN``` copied in step 4.1
![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sqs-permission-conditions.png)

4.11. Click the ```Add Condition``` button

4.12. Click ```Add Permission``` to save

#### Step 5: Test

5.1. Navigate to the SNS topics list

5.2. Check the checkbox next to the topic created in step 1.3

5.3. Click ```Publish to topic```

5.4. Enter a message and click ```Publish message```

![SQS Menu](https://s3.amazonaws.com/ns-toolkit-docs/sns-publish-msg.png)

5.5. Navigate back to the SQS console, filter by the queue name, and the count of ```Messages Available``` should have increased.

For more details: [Sending Amazon SNS Messages to Amazon SQS Queues](http://docs.aws.amazon.com/sns/latest/dg/SendMessageToSQS.html)

## Storage
The `ns-toolkit.storage` namespace provides functions for making S3 buckets, saving, deleting, and getting files. Below are examples to get started:

#### How to call the storage functions
```clojure
(require '[ns-toolkit.storage :as stg])
;; Make a new S3 Bucket
(stg/mk-bucket! "my-dir")
;; Save, but don't overwrite the file named "foo"
(stg/save! "Some data" "my-dir" "foo")
;; Save, overwriting the file named "foo"
(stg/save! "Some data" "my-dir" "foo" :overwrite true)
;; Save a file with a UUID generated for the name, note that byte data works too
(stg/save! (.getBytes "Some data") "my-dir" nil "")
;; Save a file named "foo" and specify the content type
(stg/save! "Some data" "my-dir" "foo" :content-type "application/pdf")
;; Check if a file named "foo" exists
(stg/file-exists? "my-dir" "foo")
;; Delete the file named "foo"
(stg/delete! "my-dir" "foo")
;; Get the contents of a file named "foo" as a string
(stg/get-file "my-dir" "foo")
```

## Finance

The `ns-toolkit.finance` namespace provides functions for dealing with financial
instruments. We can currently calculate the amortization table and payment schedule
on all loan types in Encompass. Loan amounts should include all fees
and finance charges.

### Calculate a Monthly Payment

Given a loan amount, interest rate, and term (in months), we can solve for a fixed monthly payment.

```
Given:
l = loan amount = $230,000
i = interest rate = 2.875%
n = loan term in months = 360
```

then it would be called:

```clojure
(ns-toolkit.finance/payment 230000 2.875 360)
=> 954.2526972750375
```

### Calculate Amortization Table

The function:

```clojure
ns-toolkit.finance/amortization
```

will take a map of the inputs to describe a loan, and then convert
that into the schedule of payments with the breakdown of each payment into
its principal and interest as well as the MI to be paid on top of that.

Since there can be many types of loans, fixed, ARM, FHA, interest-only, the
input will have a few common parts, and then a few optional parts - based on
the type of loan to be processed.

For a typical conforming, fixed-rate loan, the input will look something like
this:

```clojure
  { :loan-amount 300000
    :rate 2.625
    :loan-term-months 360
    :property-valuation 350000
    :mi-monthly-premium-amount 121.50
    :units-financed 1
    :property-usage "Primary Residence"
    :loan-type "Conforming" }
```

For a conforming, adjustable-rate mortgage, the only change is to the
`:rate` parameter can be a sequence of rate periods, defined as:

```clojure
  { :loan-amount 300000
    :rate [{:months 12 :rate 3.750 :interest-only true}
           {:months 12 :rate 3.750}
           ...]
    :loan-term-months 360
    :property-valuation 350000
    :mi-monthly-premium-amount 121.50
    :units-financed 1
    :property-usage "Primary Residence"
    :loan-type "Conformibng" }
```

There are defaults for some common values, and so they can be omitted:

```clojure
    :units-financed 1
    :property-usage "Primary Residence"
    :loan-type "Conforming"
```

To add an _interest only_ section to the beginning of each of these loans,
simply add the `:interest-only` tag, measured in months, to the input:

```clojure
  { :loan-amount 300000
    :rate 2.625
    :loan-term-months 360
    :interest-only 60
    :property-valuation 350000
    :mi-monthly-premium-amount 121.50
    :units-financed 1
    :property-usage "Primary Residence"
    :loan-type "Conforming" }
```

If the loan is an FHA loan, then the `:loan-type` will be "FHA" and
an additional tag needs to be added:

```clojure
  { :loan-amount 300000
    :rate 2.625
    :loan-term-months 360
    :interest-only 60
    :property-valuation 350000
    :mi-monthly-premium-amount 121.50
    :units-financed 1
    :property-usage "Primary Residence"
    :loan-type "FHA"
    :endorsement-date #joda "2005-01-05T12:31:54"
    :closing-date #joda "2005-01-15T12:31:54"
    :ufmip-financed true }
```

where `:ufmip-financed` is a boolean indicating if the Upfront MI Premium
is being financed. The MI payment structure for FHA depends on knowing this,
and if it's not present, then it will be assumed to be `false`.

Additionally, the `:closing-date` is really the _later_ of the closing or
dispersement date - as defined by FHA. In early 2017, FHA changed their
mothly MI rates, and the cut-over date for those rate changes is based on the
later of the closing or dispersement date. For Emcompass loans, we are using
the RESPA trigger date (Field 3142) as that's the date the business wanted to
use.

If the loan is a USDA loan, then the `:loan-type` will be "USDA" and
an additional tag needs to be added:

```clojure
{ :loan-amount 300000
  :rate 2.625
  :loan-term-months 360
  :interest-only 60
  :property-valuation 350000
  :mi-monthly-premium-amount 121.50
  :units-financed 1
  :property-usage "Primary Residence"
  :loan-type "USDA"
  :commitment-date #joda "2005-01-05T12:31:54" }
```

where `:commitment-date` is a Joda date/time which will be used to determine
the USDA's Guarantee Premium as well as the Annual Premium.

The return is the amortization schedule for the complete loan to the caller. This
will be a sequence of maps of the form:

```clojure
  { :amount 943.21
    :balance 195824.38
    :interest 490.44
    :ltv 93.25
    :mi 100
    :num 12
    :principal 352.77 }
```

and should match what is generated from Encompass exactly. The payment is the
sum of the `:amount` and the `:mi` values - for each month.

### Calculate APR

The function:

```clojure
ns-toolkit.finance/apr
```

will take a map of the inputs to describe a loan, and then convert
that into the calculated APR value for the loan.

The input is the same as that for the Amortization function, above, and the
returned value is just the APR.

## Prospector

The `ns-toolkit.prospector` namespace provides functions for categorizing lead data into one of
many tiers for the purpose of ranking. The tier criteria logic has been ported from
[Offers Service's code](https://github.com/Guaranteed-Rate/LeadManager/blob/develop/LeadManager/Domain/Leads/LeadModel.cs#L1434-L1934).

### Usage

The `->tier` function takes a map as described in its documentation, and returns a map
of tier information if the data fit into a specific tier, otherwise `nil`. There are 5 top-level
tiers, each having several sub-tiers e.g. 1a, 1b, 2a, 2b, ... 5f. The tiers also have a linear
numerical rank where the highest tier has the highest integer rank.

```clojure
{ :tier 1
  :sub-tier "a"
  :rank 100 }
```

### Evolution

The code is currently only exercised by tests, and there are decisions and enhancements
to be made before it's ready for production use.

* Dynamically/optionally resolve loan data from other services, using reference IDs passed
  by the caller
  * AUS data
  * Pricing data
  * Lip Service will hopefully be a central source for resolving data
* Expose RESTful API

Further project documentation is on
[wiki.guaranteedrate.com](http://wiki.guaranteedrate.com/pages/viewpage.action?pageId=16027993).
