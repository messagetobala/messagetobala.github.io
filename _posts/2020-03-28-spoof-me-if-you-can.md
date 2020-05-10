---
layout: post
title:  "Spoof Me If You Can"
date:   2020-03-28 13:19:16 +0530
categories: email
excerpt: In this article, we will look at "What Email Spoofing is ?" and the preventive measures like SPF, DKIM and DMARC that were introduced to prevennt it.
---
[Email](#email)

[Email Spoofing](#email-spoofing)

[Some basic terms and concepts](#some-basic-terms-and-concepts)

[Understanding Email Message Flow](#understanding-email-message-flow)

[SPF](#spf)

[DKIM](#dkim)

[Can SPF and DKIM alone prevent Email Spoofing ?](#can-spf-and-dkim-alone-prevent-email-spoofing-)

[DMARC](#dmarc)

[Viewing the results of SPF, DKIM and DMARC](#viewing-the-results-of-spf-dkim-and-dmarc)

[Conclusion](#conclusion)

## Email

Email could be considered as the first social media platform that gained widespread usage. Invented primarily  for researchers spread across the globe to collaborate and share information, slowly it found its way into every common man’s life. I still remember the days, where we used to forward "Good Morning"  messages to friends via Email.  Though  today "Gmail" and "Yahoo" groups have been replaced by "WhatsApp" groups, Email is still widely used for official communication.  All the organizations that we interact with, like Banks, E-Commerce sites, Government agencies etc use Email as the preferred way of communication making Email  an indispensable part of our life.  

Unfortunately the popularity of Email has also made it,  the number one preferred way for hackers and spammers to carry out their malicious activities.  One of the techniques used by both these groups is very simple and is called "Email Spoofing".   In this article, we will look at what "Email Spoofing" is and look at the preventive measures like SPF, DKIM and DMARC that were introduced to stop it.

## Email Spoofing

"Email Spoofing" is  a technique where hackers/spammers send specially crafted email messages to unsuspecting persons.  When the recipient of the message opens it for reading, it would look like the message has come from an email address of a person or organization know to them, thus making the recipient to trust the contents of the message. Often these messages would have some malicious attachments, downloading which would allow the hacker to gain control over the recipient’s system. Or, the message may request the recipient  to share some sensitive information by replying to the email (The reply could made to be delivered to an email address that is different from the ‘From’ email address) or  by clicking on a website link included in the message.

## Some basic terms and concepts

Before we dive into the details, let us look into some basic terminology related to Email.

- Email Service Providers

These are organizations that provide email functionality to other organizations or to individual users. Google , Yahoo, Microsoft are examples of email service providers.  For example, as an individual user, I can use  the free email service provided by Microsoft and get an email address in ‘outlook.com’ domain for my personal use.  If I am running my own organization with my own domain, I can use Microsoft’s O365 service to get email service for my organization.

- Email Client

This is  a software application that users use to access email messages sent to them and to compose and send new messages.  Email Clients use IMAP and SMTP protocols to retrieve and send messages. When we install the email client, we would have to configure it with our email address, password and details of our email service provider. Here, by Email Clients I am referring to desktop based clients like Outlook, Thunderbird etc.  Websites like "gmail.com" ,    "yahoo.com" do not use IMAP or SMTP. They are based on HTTP.
           
- Email Server

This is a software application that email service providers run to provide email functionality.  There are several important pieces to a email server.  The most important ones are,   
   
&nbsp;&nbsp;&nbsp;&nbsp;i) IMAP Server
    
&nbsp;&nbsp;&nbsp;&nbsp;Internet Message Access Protocol (IMAP) protocol defines how email clients can connect to a email server and  retrieve the email messages received. Every email server should have an implementation of this protocol, so that email clients can connect to it and download email messages. IMAP servers usually listen on port 143 or 993 for incoming requests.
        
&nbsp;&nbsp;&nbsp;&nbsp;ii) SMTP Server
    
&nbsp;&nbsp;&nbsp;&nbsp;This part of the email server is responsible for the following use cases.
           
&nbsp;&nbsp;&nbsp;&nbsp;Receiving the messages that are being sent by its own users and deliver them to the specified recipients. 
 
 &nbsp;&nbsp;&nbsp;&nbsp;Receive messages that are being sent to its users from other email servers.
        
&nbsp;&nbsp;&nbsp;&nbsp;SMTP stands for Simple Mail Transfer Protocol and  defines the way on how new messages should be submitted to a SMTP server.  The protocol specifies a set of commands using which client can pass on information like the sender, recipients and the actual message.  After sending a command, the client should wait for a response from the server before sending the next command. Some important commands are,
          
&nbsp;&nbsp;&nbsp;&nbsp;**HELO/EHLO** -  In this command the client specifies its hostname or ip address.
          
&nbsp;&nbsp;&nbsp;&nbsp;**MAIL FROM** - For specifying the sender email address.
            
&nbsp;&nbsp;&nbsp;&nbsp;**RCPTTO** - For specifying recipient email address. For each recipient the email client should send a separate RCPT TO command
             
&nbsp;&nbsp;&nbsp;&nbsp;**DATA** - This indicates that the client will next send the actual message in MIME format.
      
&nbsp;&nbsp;&nbsp;&nbsp;SMTP servers listen on port 587/465 (for use case 1) and port 25 (for use case 2). After receiving a message SMTP servers usually hand it over to another component called MTA for delivery.

&nbsp;&nbsp;&nbsp;&nbsp;iii) MTA
    
Mail Transfer Agent (MTA) is the part of the email server that delivers the new messages to  the intended recipients. If the recipients are in the same email server, it just needs to persist the message on the recipients mailbox location. If the recipient is on another email server, it would need to connect to that email server and deliver the message.
    
- MIME

Multi-purpose Internet Message Extension (MIME) is a standard that defines the format of an email message. As per this standard, a email message consists of a header part followed by a empty line which then followed by the body part.  The body part contains the actual content of the message.  The header part provides details like the from email address, subject , date etc.  One of the important headers is the "From" header. 
                                                                                                    
#### A sample email message                                      
```                        
From: user_alice@gmail.com
To: user_bob@outlook.com
Message-ID: <459848100.0.1587815129693@[192.168.1.2]>
Subject: Spoof Me If You Can
Date: Sat, 25 Apr 2020 17:15:29 +0530 (IST)
MIME-Version: 1.0
Content-Type: text/plain; charset=us-ascii
Content-Transfer-Encoding: 7bit

This is a sample email message. Email messages consists of header part and one or more body parts.       
``` 

- MX DNS Record
"Mail Exchange" (MX) record is DNS record type that gives us the hostname  of the email server of a domain.  For example, if the MTA running in Gmail email server want to deliver a message to a recipient with email address "user@outlook.com", it needs to know the IP address of the "outlook.com" domain’s email server.  This information is published via DNS MX records.
 

#### MX record of outlook.com domain obtained via dig command
```                     
#> dig outlook.com mx +short
5 outlook-com.olc.protection.outlook.com.
``` 
    
## Understanding Email Message Flow

Let us see with an example, on how all the pieces mentioned in the previous section fit together. Assume that we have two users Alice and Bob.  Alice wants to send a message to Bob from her email account **"user_alice@gmail.com"** . Bob’s email address is **"user_bob@outlook.com"** .  For understanding purpose, let’s assume Alice is using SWAKS  as her email client. It is a command line tool using which we can send message.  The benefit is we can see the actual SMTP commands that are being sent when a message is sent. 

#### A SWAKS command to send messages with authentication
```
>./swaks --server smtp.gmail.com:465 \
--auth-user "rbkrbkrbkrbk7@gmail.com" --auth-password "mynxswhsogohdmhr" \
--to user_bob@outlook.com --from "user_alice@gmail.com" \
--h-from "User Alice<user_alice@gmail.com>"\
--h-subject "Need Urgent Help" \
--body "Stuck in airport. Lost my baggage. Need 2K urgently. Pls transfer to my a/c 12345" 
--tlsc 
```

When Alice run’s the above command the following things happen,


- Step 1

Email Client connects to Gmail SMTP server,  uses the  credentials provided to authenticate and submits the message  for delivery using SMTP protocol. 

The SMTP commands exchanged would look like below. Please see my comments included between /* and */

```
220 smtp.gmail.com ESMTP n13sm7609611qtf.15 - gsmtp
EHLO automation1.localdomain
250-smtp.gmail.com at your service, [34.200.131.8]
250-SIZE 35882577
250-8BITMIME
250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN OAUTHBEARER XOAUTH
250-ENHANCEDSTATUSCODES
250-PIPELINING
250-CHUNKING
250 SMTPUTF8
/* SMTP Authentication Begins */
AUTH LOGIN
334 VXNlcm5hbWU6
/* Email Address being sent in Base64 encoding */
cmJrcmJrcmJrcmJrN0BnbWFpbC5jb20=
334 UGFzc3dvcmQ6
/* Password being sent in Base64 encoding */
bXlueHN3aHNvZ29oZG1ocg==
235 2.7.0 Accepted  /* Authentication Successful */
/*MAIL FROM command indicates the originating email address */
MAIL FROM:<user_alice@gmail.com>  
250 2.1.0 OK n13sm7609611qtf.15 - gsmtp
/*Recipient email address */
RCPT TO:<user_bob@outlook.com> 
250 2.1.5 OK n13sm7609611qtf.15 - gsmtp
DATA
354  Go ahead n13sm7609611qtf.15 - gsmtp
Date: Sun, 26 Apr 2020 06:01:56 +0000  /*Start of actual message in MIME format */
To: user_bob@outlook.com
From: User Alice<user_alice@gmail.com>
Subject: Need Urgent Help
Message-Id: <20200426060156.004553@automation1.localdomain>
X-Mailer: swaks v20190914.0 jetmore.org/john/code/swaks/  /*Header finsishes*/

Stuck in airport. Lost my baggage. Need 2K urgently. Pls transfer to my a/c 12345


.
250 2.0.0 OK  1587880916 n13sm7609611qtf.15 - gsmtp
QUIT
221 2.0.0 closing connection n13sm7609611qtf.15 - gsmtp
```

- Step 2

The MTA in Gmail’s email server picks up the message , connects to the SMTP  server of the  domain "outlook.com" on port 25 and delivers the message. The SMTP exchange between MTA in Gmail’s email server and the SMTP server of "outlook.com" would be as below.

```
EHLO mail-pl1-f196.google.com
250-BO1IND01FT010.mail.protection.outlook.com Microsoft ESMTP MAIL Service ready at Sat, 25 Apr 2020 11:54:45 +0000 at your service
250-BO1IND01FT010.mail.protection.outlook.com Hello [182.65.14.29]
250-SIZE 35882577
250-8BITMIME
250-STARTTLS
STARTTLS
220 2.0.0 Ready to start TLS
EHLO mail-pl1-f196.google.com
250-BO1IND01FT010.mail.protection.outlook.com Hello [182.65.14.29]
250-SIZE 35882577
250-8BITMIME
250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN OAUTHBEARER XOAUTH
250-ENHANCEDSTATUSCODES
/*MAIL FROM command indicates the originating email address */
MAIL FROM:<user_alice@gmail.com>
250 2.1.0 OK
/*Recipient email address */
RCPT TO:<user_bob@outlook.com>
250 2.1.5 OK
DATA
354  Go ahead i10sm8030721pfa.166 - gsmtp
Date: Sat, 25 Apr 2020 17:15:29 +0530 (IST)
From: User Alice<user_alice@gmail.com>
To: user_bob@outlook.com
Message-ID: <459848100.0.1587815129693@[192.168.1.2]>
Subject: Spoof Me If You Can
MIME-Version: 1.0
Content-Type: text/plain; charset=us-ascii
Content-Transfer-Encoding: 7bit  /*Header finsishes*/

Stuck in airport. Lost my baggage. Need 2K urgently. Pls transfer to my a/c 12345
.
250 2.0.0 OK 
QUIT
221 2.0.0 closing connection 
```

- Step 3

The Email  Client in my friends laptop uses IMAP to fetch the message and displays it as soon as it arrives. 
     
Below is a screen shot of how the message would be  displayed in  Bob’s email client.
      
      

![](https://paper-attachments.dropbox.com/s_22872381B5E0C682806D56C31A547CD4B1E74782917704884319BBD6C1B203D3_1588310134582_image.png)


The actual content of the message in MIME format would be something like below.
      
```
Date: Sat, 25 Apr 2020 17:15:29 +0530 (IST) 
From: rbkrbkrbkrbk7@gmail.com 
To: user_bob@outlook.com 
Message-ID: <459848100.0.1587815129693@[192.168.1.2]> 
Subject: Spoof Me If You Can 
MIME-Version: 1.0 
Content-Type: text/plain; charset=us-ascii 
Content-Transfer-Encoding: 7bit

This is a sample email message. Email messages consists of header part and one or more body parts.
```
      
 Now consider the difference between Step 1 and Step 2. Both uses SMTP. The difference is, in Step 1,  the email client uses credentials  of Alice to authenticate with the Gmail Server. The Gmail Server validates the password sent. It knows for sure that message is coming from Alice. So it accepts the message for delivery.
 
In Step 2, there is no authentication.  When the email server of the domain "outlook.com" receives the message from  Gmail’s MTA,  the "MAIL FROM" smtp command and the "FROM" header in the MIME message will have "user_alice@gmail.com". But there is no way for it to verify that the message was indeed sent by "Alice".  If "outlook.com" email server enforces that it will accept message from only authenticated users, no one outside of "outlook.com" would be able to send messages to users using "outlook.com" email address,  as "outlook.com" email server will not be having password information of users using other email service providers.
 
So, it is possible for any one to connect to "outlook.com" email server and submit a message that has "user_alice@gmail.com" in the "MAIL FROM" smtp command and the "FROM" MIME header. 

#### A SWAKS command to send messages without authencation
```
>./swaks --server outlook-com.olc.protection.outlook.com:25 \
--to user_bob@outlook.com --from "user_alice@gmail.com" \
--h-from "User Alice<user_alice@gmail.com>"\
--h-subject "Need Urgent Help" \
--body "Stuck in airport. Lost my baggage. Need 2K urgently. Pls transfer to my a/c 12345" 
--tls
```
Any one can run the above  command and  Bob would see a message like below in his inbox exactly like the first message.


![](https://paper-attachments.dropbox.com/s_22872381B5E0C682806D56C31A547CD4B1E74782917704884319BBD6C1B203D3_1588310190961_image.png)


#### SMTP transcript of above command
```
EHLO automation1.localdomain
250-BO1IND01FT005.mail.protection.outlook.com Hello [34.200.131.8]
250-SIZE 157286400
250-PIPELINING
250-DSN
250-ENHANCEDSTATUSCODES
250-8BITMIME
250-BINARYMIME 
250-CHUNKING
250 SMTPUTF8
MAIL FROM:<rbkrbkrbkrbk7@gmail.com>
250 2.1.0 Sender OK
RCPT TO:<user_bob@outlook.com>
250 2.1.5 Recipient OK
DATA
354 Start mail input; end with <CRLF>.<CRLF>
Date: Sun, 26 Apr 2020 06:45:12 +0000
To: user_bob@testkanha2.onmicrosoft.com
From: User Alice<user_alice@gmail.com>
Subject: Need Urgent Help
Message-Id: <20200426064512.006078@automation1.localdomain>
X-Mailer: swaks v20190914.0 jetmore.org/john/code/swaks/

Stuck in airport. Lost my baggage. Need 2K urgently. Pls transfer to my a/c 12345


.
250 2.6.0 <20200426064512.006078@automation1.localdomain> [InternalId=39904541148036, Hostname=MAXPR0101MB2105.INDPRD01.PROD.OUTLOOK.COM] 8456 bytes in 0.153, 53.634 KB/sec Queued mail for delivery
QUIT
```    

Now what should Bob do on seeing the message?

If he transfers the money and the message was indeed sent by Alice, he is the best friend any one can have. But, if he transfers the money and the message was not sent by Alice, he is a victim of "Email Spoofing"   

## SPF

How can email service providers protect users like "Bob" from "Email Spoofing".  In our example above, at Step 2, we need a way for "outlook.com" email server to find out if the message being submitted is actually from "user_alice@gmail.com".   But authenticating users in "gmail.com" domain is not possible for the "outlook.com" SMTP server.  

The next best thing that can be done is checking if the message is indeed being submitted  by email server of "gmail.com" .  In our example,  when "outlook.com" email server, is processing the incoming message, as soon as it receives the "MAIL FROM" command, it can find out the "domain" from the email address and check if the IP address from where the message is submitted belongs to the domain("gmail.com" in our example) mentioned in the "MAIL FROM" command.

This approach is known as "Sender Policy Framework (SPF)". It requires that  for every domain they support, email service providers list the IP addresses from which they will deliver email messages to external recipients  via DNS TXT records.   This allows a email server to take the domain from the "MAIL FROM" command,  get the list of IP’s from which messages can be submitted for this domain and check if the IP from which the message is being submitted is in that list. If the message is not in the list, then something is wrong and it can take a corresponding action like moving the message to "Spam" folder.


### SPF TXT records

A SPF TXT record  of a domain provides two critical pieces of information. The list of IP addresses, from where email messages from the domain can be sent.  Second one is the kind of action to be taken if a message is received from an IP address that is not in the list.

If we take a look at  SPF record of "gmail.com" we get the following,

```
#> dig gmail.com txt +short
"v=spf1 redirect=_spf.google.com"
```

Here, *"v=spf1"* indicates that it is a SPF TXT record.  The *"redirect=_spf.google.com"* indicates that to get list of allowed IP’s for  "gmail.com" , you need to check the SPF record of *"_spf.google.com"*. You can consider this as very similar to HTTP redirects.

Looking at the SPF record of *"_spf.google.com"* gives us the following,

```
#> dig _spf.google.com txt +short
"v=spf1 include:_netblocks.google.com include:_netblocks2.google.com include:_netblocks3.google.com ~all"
```

Again there is nothing related to an IP address, instead we have an "include" directive. Here, *"include:_netblocks.google.com"* indicates that we have to look up at the SPF record of *"_netblocks.google.com"* which is configured as below.

```
#> dig _netblocks.google.com txt +short
"v=spf1 ip4:35.190.247.0/24 ip4:64.233.160.0/19 ip4:66.102.0.0/20 ip4:66.249.80.0/20 ip4:72.14.192.0/18 ip4:74.125.0.0/16 ip4:108.177.8.0/21 ip4:173.194.0.0/16 ip4:209.85.128.0/17 ip4:216.58.192.0/19 ip4:216.239.32.0/19 ~all"
```

Finally we see some IP address ranges. If we look at the SPF records of the other domains included in the SPF record of *"_spf.google.com"*, we will get more IP address ranges. These are the IP address ranges from which the email server of "gmail.com" will deliver email messages to other email servers.

Coming back to our example, now when the email server of "outlook.com" receives a message and sees a "gmail.com" email address in the MAIL FROM command, it can look up the SPF records of "gmail.com"  and check if the message is coming from one of the IP’s listed in the SPF records by Gmail.  If the IP is found, it can treat is a legitimate message. 

What if the IP is not found? Then the email server of "outlook.com" can treat it as a malicious/spam  message and either drop the message or mark it as "spam" and deliver it to the "spam" folder of the recipient instead of the "inbox" folder.  The "outlook.com" email server can take whatever action it wants. But there is a way in SPF records for domains to suggest how SPF validation errors for their domains should be treated. 

For example, in the SPF record of "_spf.gmail.com" we see the string  "~all" at the end.  Here the "~" stands for a "SOFT FAIL".  It is like Gmail saying "For all ip’s that does not match against the given list, take a milder failure action (like marking it as Spam but still delivering to the recipient)".  For some domains you will see a "-all" at the end of the SPF record. The "-" stands for "HARD FAIL" and  indicates that the domain wants the recipient mail server to take a strict action like dropping the message on SPF validation failures.  For example, the SPF record of "wellsfargo.com" indicates a "HARD FAIL"

```
#>dig wellsfargo.com txt +short
"v=spf1 redirect=wf.com"
#>dig wf.com txt +short
"v=spf1 ip4:167.138.239.64/26 ip4:151.151.26.128/26 ip4:151.151.65.96/27 ip4:151.151.5.32/27 ip4:159.45.132.160/27 ip4:159.45.13.96/27 ip4:159.45.78.192/27 ip4:159.45.16.64/26 ip4:159.45.87.64/26 ip4:159.45.132.160/27 -all"
```

## DKIM

In SPF we are trying to validate the sender based on IP address. "DomainKeys Identified Mail (DKIM)" provides an alternate method to validate the senders based on digital signature.  Digital signatures is based on asymmetric cryptography and it not only allows the receiver of the message to validate the sender, it also allows the receiver to validate that the message was not tampered while it was in transit. When digital signatures are used for message exchanges, it usually consists of the following steps.


- The sender calculates the hash of the message content he is going to send.
- The sender then encrypts the hash using his private key.  The encrypted hash is called the "Digital Signature" and this is also sent to the receiver along with the message contents.
- The receiver fetches the public key of the sender and decrypts the encrypted hash.
- The receiver then calculates the hash of the message contents. Now the hash calculated by receiver should match the hash obtained by decrypting the encrypted hash. If it matches, the receiver can be sure about the authenticity of the sender and the contents of the message. 

Let us go back to our example and see how DKIM uses digital signatures in email message exchanges.   The email server of "gmail.com" before connecting to the email server of "outlook.com",  would calculate the digital signature of the message.  It takes the content of the "From" MIME header and the body of the message, calculates the hash and encrypts it using its private key. It then includes this encrypted hash in a special header called "DKIM-Signature" and delivers the message to the email server of "outlook.com"

So if Alice sends a message to Bob, the message received by Bob’s email server would have an header like below.

```
DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; 
d=gmail.com; s=20161025; 
h=from:date:to:subject:message-id; 
bh=zppMSfiuNQ9a9MsEBuCGp953xYlKNtz7cMzvS0AL1N0=; 
b=EKv8itvYpAnclaWCvs+ym/kvQlTItMTZwBuGBF3vbZnEpb6HPHzWOj2H2/whGWpQZG DgLlzdGYyJ2Ucf89s5/3aSqaggdKbSL9VV1Z7K4Y00bDpsCKKhTfti/41UImDpM1+2FV 6YCtvo/qI0fojsaPvsAs17sh6PDvCo8ihOMx43slfM3MJhPyAqaN3uWcnoPa5siuhSZp 6+pavRuNWf1RuTqfPm/BCphiONWePaBo1uJM0zpZ6QeIzJp5detzA3S7ShdYF/QQQJ4m n5BIUF1VehQX4ueQdF0Us3S6E+rTtTgFqwLTOSDprzz+f12vyJMLG9sVfNrqzFcSa2QECNQg==
```

The email server of "outlook.com" on receiving the message, would check if the message contains "DKIM-Signature". If the message contains the DKIM signature, it will fetch the public key of "gmail.com", decrypt the encrypted hash and compares it with the hash value it calculates.  If both the hashes match,  email server of "outlook.com" can treat it a legitimate message. If the hashes do not match, it is a spoofed message and appropriate action like dropping the message or delivering it to the "SPAM" folder could be done. 

Now to perform this validation, the email server of  "outlook.com" needs to know the public key corresponding to the private key used by the email server of "gmail.com" to generate the signature. Similar to SPF, this information is published via special DNS TXT records.

### DKIM TXT Records

Before looking at  how the public keys are published via DNS TXT records, let us take a look at the "DKIM-Signature" header which is added to the message.  In the DKIM header example you can see the following fields,

 *   d -  Domain field. This indicates the domain of the sender.
 *   s  - This field is known as selector. We could think of like the "id" of the private key which was used to generate the signature.
 *   h -  This field gives us the list of headers which were included while computing the signature. The "From" header is the only required header that needs to be included while computing the signature. The sender can optionally include other fields.
 *   bh -  Body Hash. This is the hash of the message body.
 *   b  -  This is the digital signature which is nothing but the hash of the headers and body encrypted using a private key.
 *   a  -   Specifies the algorithm used for signing and calculating hash.    

To fetch the public key, the receiver should query the TXT record for the string "<selector>._domainkey.<domain>". In our example it is "20161025._domainkey.gmail.com". If we query we get the following. Here the field "p" gives the public key corresponding to the private key used by the email server of "gmail.com" to sign the message.

```
#>dig 20161025._domainkey.gmail.com TXT +short
"k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAviPGBk4ZB64UfSqWyAicdR7lodhytae+EYRQVtKDhM+1mXjEqRtP/pDT3sBhazkmA48n2k5NJUyMEoO8nc2r6sUA+/Dom5jRBZp6qDKJOwjJ5R/OpHamlRG+YRJQqR" "tqEgSiJWG7h7efGYWmh4URhFM9k9+rmG/CwCgwx7Et+c8OMlngaLl04/bPmfpjdEyLWyNimk761CX6KymzYiRDNz1MOJOJ7OzFaS4PFbVLn0m5mf0HVNtBpPwWuCNvaFVflUYxEyblbB6h/oWOPGbzoSgtRA47SHV53SwZjIsVpbq4LxUW9IxAEwYzGcSgZ4n5Q8X8TndowsDUzoccPFGhdwIDAQAB"
```

## Can SPF and DKIM alone prevent Email Spoofing ?

If a email server performs SPF and DKIM validation, can it prevent all cases of email spoofing and protect it users?   In our example,  when "outlook.com" email server receives a message where the "MAIL FROM" command says "user_alice@gmail.com", it looks up the SPF records of "gmail.com", validate the IP and is going to allow the message only if the IP is in the list of IP’s published by Gmail.  So can we say, "Yes, SPF prevents Email Spoofing".  Unfortunately, the answer is "NO". 

To bypass SPF, a spammer can simply buy a domain say "spammer.com", set up SPF records for that domain,  use the address "user_alice@spammer.com" in the "MAIL FROM" command and send a message (which has the  "From" MIME header as "user_alice@gmail.com") from an IP address that is listed in the SPF record of domain "spammer.com". In this case ,  "outlook.com" email server receives a message where the "MAIL FROM" command says "user_alice@spammer.com". It looks up the SPF record of "spammer.com", sees that the IP from where the message is coming is listed in the SPF record and is going to treat the message as a legitimate one ad deliver it to Bob’s inbox.

Similarly, to bypass DKIM, a spammer can publish public keys for his domain "spammer.com" and send a spoofed message that will have "From" MIME header as "user_alice@gmail.com" with a valid "DKIM-Signature" header.  He just needs to set the domain field in the DKIM signature to his domain "spammer.com".

How can we handle such a scenario?  For SPF, the problem comes from the fact that the are two "From" email addresses.   One comes from the "MAIL FROM" SMTP command and the other address is in the  "From" MIME header .  It is only the email address in the "From" MIME header that is displayed to the recipients when they see the message in the email client. 

In case of DKIM, the problem is because the domain used to look up the public key comes from "DKIM-Signature" header and it could have any domain name. It need not necessarily match the domain of the email address in the "From" MIME header.  

## DMARC

Domain Based Message Authentication, Reporting and Conformance (DMARC) protocol builds on top of SPF and DKIM and plugs the loophole with SPF and DKIM that we saw in previous section.
As per DMARC, for a message to be considered legitimate it needs to satisfy at least one of the two conditions listed below,

- The message should pass SPF validation and the domain of the email address in "MAIL FROM" SMTP command should match the domain of the email address in "FROM" MIME header.
- The message should pass DKIM validation and the domain mentioned in the DKIM-Signature header should match the domain of the email address in "FROM" MIME header.

Now there is no way for a spammer to send a message to Bob pretending to be Alice. Because the domain in the "FROM" header needs to match the domain in "MAIL FROM" or  the domain in the "DKIM-Signature" header both of which can only be the spammer’s own domain "spammer.com". So if he wants to send a message to Bob, the only way is to set the "From" MIME header to an email address in "spammer.com". 

A domain can indicate whether it wants the email servers to perform DMARC validation for messages supposedly coming from its email servers via DNS TXT records. To look up the DMARC TXT record of a domain we need to use the string "_dmarc.<domain>" . 

For example, the DMARC record of "gmail.com" looks like below.

```
#>dig _dmarc.gmail.com TXT +short
"v=DMARC1; p=none; rua=mailto:mailauth-reports@google.com"
```

Here,
   p  - Indicates the action that the receiving email server should take when DMARC validation fails. It can have the following values none (no action), reject  and quarantine. (It is a surprise that gmail.com has set the action has "none" and not "reject")
   
  rua - Indicates  an email address to which an aggregate report about the result of every DMARC validation done for that domain can be sent.  In the example above, "gmail.com" is requesting other email service providers to send an aggregate report to the address "mailauth-reports@google.com". The report will have the DMARC validation results for every message received from "gmail.com" over a period of time.  This will help "gmail.com" to see if it sending emails from any IP address that it has not included in the SPF record and correct it.

## Viewing the results of SPF, DKIM and DMARC

We saw about SPF, DKIM and DMARC and the validations they perform.  But how does a user reading a email message  in a email client know whether the message he is reading has passed SPF, DKIM and DMARC validations or not ?   The results of all these validations are included in the message in a special header  called "Authentication-Results". 

For a legitimate message the header would look like below,

> Authentication-Results: **spf=pass** (sender IP is 209.85.160.195) smtp.mailfrom=gmail.com; outlook.com; 
> **dkim=pass** (signature was verified) header.d=gmail.com;outlook.com; **dmarc=pass** action=none 
> header.from=gmail.com;compauth=pass reason=100


For a spoofed message the header would look like below,

> Authentication-Results: **spf=softfail** (sender IP is 34.200.131.8) smtp.mailfrom=gmail.com; outlook.com; 
> **dkim=none** (message not signed) header.d=none;outlook.com; **dmarc=fail** action=none 
> header.from=gmail.com;compauth=fail reason=001


In all  email clients there would be a way to look at the headers of the message. For example in gmail you can see the steps [here](https://support.google.com/mail/answer/29436?hl=en). For Outlook the instructions are available [here](https://support.office.com/en-us/article/view-internet-message-headers-in-outlook-cd039382-dc6e-4264-ac74-c048563d212c). Unfortunately , there is no better way to look at the authentication results. 

It will be of great help, if the email clients show the results of these validations along with other information like From, Subject etc via an visual indicator like the "Green padlock" we see in the browsers when we visit a valid "HTTPS" site.

## Conclusion

Now coming back to the most important question of this article. What should Bob do on seeing the below message?


![](https://paper-attachments.dropbox.com/s_22872381B5E0C682806D56C31A547CD4B1E74782917704884319BBD6C1B203D3_1588310190961_image.png)



We saw that  even the widely used "gmail.com" domain has set the DMARC action has "none" and the SPF action has "SOFT FAIL".  This means that in our example, when "outlook.com" receives a message supposedly from "user_alice@gmail.com", it will perform DMARC validation. But even if the DMARC validations fail,  since the action is set has "none" it can deliver the message to Bob’s inbox.

It will be best for Bob and all of us, if we develop the habit of checking the "Authentication-Results" header  before responding to any mails that raise the slightest of suspicision. And once we develop this habit we can with confidence, challenge the spammers and say ***"Spoof Me  If You Can"***
