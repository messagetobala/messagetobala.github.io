# Salt, Pepper And Passwords

## “Passwords” - Something which only the user should know

Passwords are the first line of defense  in preventing malicious actors  from impersonating us and accessing the applications we use.  Although we are seeing  applications that are relying solely  on SMS based OTP authentication, instead of passwords(Which is a very bad idea as [Twitter CEO found out](https://www.wired.com/story/jack-dorsey-twitter-hacked/) ),  a vast majority of the application out there rely on passwords as the first factor of authentication.   It is very important that the passwords are stored securely by the application we use.  For any company, it is a disaster if the passwords of their users fall into the wrong hands.  In this article, I will share some of the things I learnt about how applications should store and manage passwords in a secure way. 

## Threats and Requirements

First, let us list down the threats from which we need to protect the passwords and the requirements that our implementation  should satisfy.

**Threats**

- If a malicious actor, gains access to our database they should not be able to figure out the passwords of the users.
-  A malicious actor, should not be able to guess a user password by trial and error.
-  A malicious employee, with access to production systems should not be able to figure out the passwords of the users.

**Requirements**

-    Application should be able to authenticate the user by comparing the password entered by the  user in the login page and the stored version of the password in the database.
-    Application should provide a way for the user to reset his password in-case he has forgotten the password. 

## Encryption vs Hashing

The first question is, **How to store the passwords in the database?** It is obvious that it should not be stored in plaintext.  We should store it in a way such that even those with access to the database cannot figure out the password of a user, by looking at the stored version of the password in the database.   This gives us two options, **“Encryption”** and **“Hashing”**
 
## Encryption

Encryption is a way in which a string S1 is transformed (aka encryption)  into another string S2 by using another string K1 called a Key.     Now the important thing to understand is the transformation is reversible. So we can use the Key K1 and transform (aka decryption) the encrypted string S2 back to the original string S2.  

For example, in the below snippet we are using "openssl” utility to encrypt a string “password@123” using the key string “SecretKey”.  We then decrypt the encrypted string to get back the original string. The algorithm used here is “AES-256-CBC”. It is only for an example. There are other better  algorithms like AES-GCM which should be used in production.


    #>echo -n "password@123"  > /tmp/password.txt
    #>openssl aes-256-cbc -a -pass pass:SecretKey -in /tmp/password.txt > /tmp/encrypted.txt
    #>aes-256-cbc -d -a -pass pass:SecretKey -in /tmp/encrypted.txt 
    password@123

   
So, how can we use encryption for storing user passwords? . When the user registers for our application, by providing the username and password, we can encrypt the password provided by the user with a key and store the encrypted password in our database.  Next time when the user logs in, we can read the encrypted password from database, decrypt it using the same key and compare it with the password entered in the login page. If both matches, the user is authenticated and we allow him to login.
   
## Hashing
Hashing is a process in which a string S1 is transformed into another string S2. Unlike encryption, the transformation process in hashing is not reversible. That is,  given hashed string S2 we cannot get back the original string S1.  Another interesting point to know is that the transformed string S2 will always be of a fixed length that depends on the hashing algorithm. 

Some common hashing algorithms are,

- md5sum -  Transforms a string S1 of any length into a string S2 of size 16 bytes.
- sha-1        -  Transforms a string S1 of any length into a string S2 of size 20 bytes.
- sha-256   -  Transforms a string S1 of any length into a string S2 of size 32 bytes.
- sha-512    -  Transforms a string S1 of any length into a string S2 of size 64 bytes.

In the snippet below, we are hashing the text “password@123” using sha-256 algorithm. The output is shown as a Hex String (Each byte is represented as two characters) of length 64 characters.


    #>echo -n "password@123"  > /tmp/password.txt
    #>shasum -a 256 /tmp/secret.text
    2b217fd26f0506d7cfe87e08483838fe8bf130ce6b3a987d94adfd3d043454a5

So, how can we use hashing for storing user passwords? At the time of registration , we could hash the password entered by the user and stored the hashed password in the database. When user logs in , we can hash the plaintext password entered by the user and compare it with the hashed password from database. If both matches, we allow the user to login.

Since, the output from hashing is of a fixed length we can have two strings S1A and S1B for which the hashing algorithm returns the same string S2. This is called a hash collision. 

Will collision cause a problem? For example, assume that the strings S1A and S1B give the same hashed text as output. User A set’s his password to string S1A. But another malicious user is trying to login as user A. The malicious  user randomly uses the string S1B as password. Since the output from the hashing algorithm for both S1A and S1B is same , we would allow the malicious user to login as user A.  So does the collision property make hashing useless for storing passwords?

The answer lies in probability. The chance of getting a collision depends on the number of hashed strings we store and the length of the output from the hashing algorithm.  The mathematics of calculating this probability is beyond my grasp. But it is considered that if we use a hashing algorithm whose output length is greater than or equal to 256 bytes, the chances of getting a collision is almost zero.   So collision is normally not considered as an issue.  For this reason sha-256  algorithm is widely used for storing passwords.

## Encryption vs Hashing - The winner is

***So should we use encryption or hashing to store passwords?***  In encryption technique, we have a new entity called key which is used for encryption and decryption. Now this key also needs to be stored securely and should be accessible for our application code to encrypt and decrypt passwords.  Just like passwords we need to worry about storing this encryption key securely. If the attacker has infiltrated our systems and gained access to our database, it is very likely that he has gained access to the encryption/decryption key also. So if the attacker has access to key and the database he can use the key to decrypt the passwords stored in the database. This is a drawback of using encryption to store passwords.

Another drawback of encryption technique is that it does not protect against threats from malicious employee. Let us say we have an malicious employee in the role of  Site Reliability Engineer (SRE) who has access to both the database and the encryption/decryption key.  He could very easily use the key and decrypt the passwords stored in the database.  But in case of hashing technique, there is no way to get the original password from the hashed password stored in the database.

For these two reasons, hashing technique is normally used to store passwords.

## Time is precious

In the previous section, I had mentioned “***there is no way to get the original password from the hashed password stored in the database***”.  While the hashing algorithm does not allow us to get the original text from the hash of the text, is there any other way of breaking it?   Can the attacker guess the user password from the hashed passwords by trial and error?

Let us say the attacker has access to our application database. From the database, he can get the username and hashed password. From the length of the hashed password, he can find out the hashing algorithm.  Now using the same algorithm, the hacker can calculate  the hash of all possible strings and compare the hash of every string with all the hashes available in the database. If there is a match, the hacker has figured out the password of that user. 

This method requires that the attacker calculates the hash of all possible strings.  “All possible strings”  means all combinations of characters available in all the languages and of all possible lengths. In theory this might sound like an impossible task. But in terms of passwords, most of us use passwords with a maximum length of 10-15 characters, the password would be usually English words or their variants with few additional special characters.  Calculating the hashes of all possible strings of maximum length of 15 characters , made up of English alphabets and special characters is not a time consuming one. In fact there are hash databases readily available which provide the hash value of common English words and their variants for different hashing algorithms. 

For example see this site https://md5decrypt.net/en/Sha256.  You can go here and look up the hash "2b217fd26f0506d7cfe87e08483838fe8bf130ce6b3a987d94adfd3d043454a5” . It will readily tell you that is the hash of “password@123”

In 2012, the database of LinkedIn was [hacked](https://en.wikipedia.org/wiki/2012_LinkedIn_hack).  From the hashed passwords found in LinkedIn database and existing pre-built hash databases, the attackers were able to determine the actual passwords and posted username/password combination of thousands of users online. 

To protect against this , **we want the task of calculating  the hash of all  possible strings time consuming**. One way could be by enforcing that the password length is very high  and passwords should not be based on any dictionary word. But both are not user friendly. There is no way a user is going to remember long passwords made up of random characters.

## Add some “Salt”

The solution to the problem is instead of asking the user to use lengthy passwords, we append some lengthy random text to the password entered by the user, thereby increasing the length of the password.   This random text is called a **“Salt**”.  This addition of random text makes the number of “all possible strings” really large, making it time consuming to calculate the hash of every possible string.  For example if the size of the salt is 64 bytes, the number of possible strings is  at least 2 ^ 512 - 1

For example, let us say user’s password is “password@123”.  While calculating the hashed password we append a long random text to the password and calculate the hash.  We then store the username, hashed password and salt in the database.  It is important that the length of the salt should be at least as long as the length of the output from the hashing algorithm. Also we should use unique random strings as salt for every user. 

To verify the password during login, we read the salt used for the user from the database , append it to the password entered by the user  and calculate the hash. If the hash matches the hashed password stored in the database we allow the user.

Below snippet shows the hash of  the string “password@12312370ede5a5926b34de07adca2229518cf573db67da77063add80e2d1d3f250d7d”.

If you now take the generated hash and go to the site https://md5decrypt.net/en/Sha256 you will not get the corresponding plain text.


    #>echo -n "password@12370ede5a5926b34de07adca2229518cf573db67da77063add80e2d1d3f250d7d6" > /tmp/secret.text 
    #>shasum -a 256 /tmp/secret.text 
    e8105552b5c48b925af72c59a6f4228a5acb891a451114ffab1c5d08375fb00b

Now with this approach even if the attacker knows the user name , salt and hashed password of every user, it makes it next to impossible for him to find out the password. To do this for every user, for every possible password string,  he needs to calculate the hash of  “password string + salt string” .  [This site](https://thycotic.force.com/support/s/article/Calculating-Password-Complexity) estimates that if password is 10 characters long, and assuming it could be from a pool of 80 characters, it would take almost 3 years on a super computer to calculate the sha-512 hash of all possible combinations.  So before the hacker can break the password, the password would have expired and the application would have asked the user to change the password. 


## Pepper

If we are using salt and the database is hacked, while it is practically impossible to determine the actual password from a password hash,  theoretically it is still possible.  It is possible because both the password hash and salt are stored in the same database.   In future we could see advances in areas  like Quantum Computing which could make the task of computing hash of large number of strings less time consuming.

So, for extra security instead of calculating the password hash based on just the “password” and “Salt”, we could include a third string while calculating the hash.  This string is called a **“Pepper**”. Unlike “Salt”, the “Pepper” should be kept secret and the same value can be used for all users. It should not stored along with the database. 

In this case, during the registration process we would calculate the hash of “password + salt + pepper”  and store resultant password hash in the database. When the user logs in, we would use the same “Salt” , “Pepper”  strings and password entered by the user to calculate the password hash. If the hash matches the one stored in database we allow the user to login.


## Making sure our own house is in order   

So far we looked at how to securely store passwords of users. But in any application , apart from the passwords of end users, there are also other critical pieces of information,  which we need to store securely and should provide access only on a need-to basis. I am listing a few such critical pieces of information and the best practices to protect them.


- Configuration Files

    Every application needs a config file.   Configuration file contains information like the URL of other endpoints the application communicates with, the credentials to access databases and internal API’s etc.  To protect such information, the permission of the configuration file should be as restrictive as possible.
    
    If your application is running in Linux/Unix like systems,  the owner of the file should be set to “root” and the group should be set to a specific group used for configuration files say “config-group”. Only “root” should have write permissions. The group should have only “read” permissions. For “others” no permission should be given.  The application can add the “config-group” as one of its auxiliary group.  This will ensure that only users with “sudo” permission are manually able to edit the configuration file while the application can read the configuration file. We could also provide read access to certain employees like SRE by adding them to the group “config-group”

- Credentials of internal systems
    We should use strong passwords for internal systems like databases.  While specifying the passwords in the configuration file, we should specify only the encrypted version. We should not provide the passwords in plain text. Application while reading the configuration, will decrypt the password and use it.
      
    We should also not use any default usernames for accessing such internal systems. For example, if we are using “MySql” database our application should not be connecting to the database as “root”  user.  We should create a separate user for each application, and for that user we should provide access only to the tables and stored procedures it uses.

      

- Securing encryption keys

    Applications would normally have one or more encryption keys which it will use to encrypt secrets like  the “Pepper” , “Database Passwords” in configuration file etc.  How do we secure them. We cannot encrypt them using another key. If we do so, we have to then secure the “other” encryption key.
      
    One solution is we can split such encryption key’s into two parts. One part we can store it in a configuration file(Need to make sure the permission of the file are restricted) and the other part we can keep it as part of the code.  So when the application starts, it will have one part of the key in its memory and the other part it will read from the configuration file.  By this method we are just making it more harder for an hacker to get hold of the “encryption key”.
      
    We could also use services  like AWS KMS . In this case , the encryption key itself will not be stored anywhere on the application server. The application  will make a call to KMS API to get the encryption key, perform encryption/decryption operations and then delete the key from memory as soon as possible.  
      
    If the encryption keys are used for very critical operations like issuing server certificates there are special devices  called Hardware Security Modules (HSM) which should be used to store the encryption keys.   


## Conclusion

Application security is very critical for any organization and any breach can turn the fortunes of a company overnight and tarnish their reputation.  For example in 2017,  personal details of millions of users  of “Equifax”, a consumer credit reporting  agency was [dumped](https://www.forbes.com/sites/kateoflahertyuk/2019/10/20/equifax-lawsuit-reveals-terrible-security-practices-at-time-of-2017-breach/#15a15d643d38) on the internet.  Hackers were able to access this information because, one of their internal web portals was made accessible over the internet.  And to make matters easy for the hackers, the admin credentials to access the portal was set as  “admin” / “admin”. 

I hope the article was useful and there were few interesting concepts that you picked up.

