# Crypthru: cryptography for organizations

Crypthru (CT) is a cryptography solution for organization workflow: it automates encrypting 
of files and messages between **parties on an organization**. 

Most cryptographic solutions target single users and email, and their usage is difficult  
to adapt to an organization workflow, partly due to their dependence on tricky  
keystore or key-rings. 

On the other side CT uses the default file system to store keys and, if you decide
to use a keystore (to identify parties) it is kept in a plain directory structure.

CT is an **open source** program: you can inspect its code, verify that there is no
back-door and consequently be 100% protected against eavesdrop. CT relies on 
[The Legion of the Bouncy Castle](https://www.bouncycastle.org/) that is also
an open source project and Java [Spring Boot](https://spring.io/projects/spring-data).

Further, CT does not use proprietary algorithms. It uses the PGP standard that results
in a military level of security. Files encrypted with CT are compatible with PGP or GPG
but automating batch and repetitive tasks is easier.

In addition to open-source license, CT is available as a corporate solution that include
strategic mentoring for the organization and assistance on how to integrate CT with
your workflow and applications. More information available writing to **crypthru@cbsolution.net**,
please inform your name email, or a phone number (with its international prefix) and
a convenient time. Just that, **nothing else**, we will contact you. 

## Peer-to-peer cryptography

CT employs [public-key cryptography](https://en.wikipedia.org/wiki/Public-key_cryptography):
each distinct party has two keys: the **public key** for writing, and the **private key**   
for reading. You generate a key-pair (public and private), keep the secret key 
**absolutely with you** and give copies of the public key to whoever wants to communicate
with you. They will encrypt the files or messages with your public key but **only you**
will be able to decrypt them. As simple as that.

Let me repeat: you shall keep your private key **absolutely with you**! You shall physically
manage it and be 100% sure you are the only person that have access to it. This is why
Crypthru make it transparent storing keys in plain text files that you can manage (for 
example keeping them a flash card) and physically move or backup in secret and safe places.

This is called peer-to-peer cryptography, and is different from end-to-end cryptography (like 
Whatsapp) that also use private-public but where the keys are managed by a third party or its
application. Such third party has the ability or can eventually be forced, to access the private
key in order to decrypt the messages.

## An example

Here is a short story to illustrate how CT works. CT is a command line application that
you shall execute on yor computer console. Despite looking a little nerdy, this will give
you full transparency on what is going on and simplify integration of CT with other
applications. If you prefer an interactive, interface you can easily wrap CT in a custom
shell.

Tarzan and Jane have set up a successful business in the jungle: they supply bananas to 
the monkeys that pay them back in coconuts collected by twisting the fruits
from the top of palm trees. Training a monkey for the job takes time and Tarzan asks Jane
(that supervise operations) to send him a weekly productivity report. This way Tarzan can
sell to Savor, as meat, under-performing monkeys in order to recover at least the investment. 

The report shall be transmitted securely (to avoid given up monkeys to attempt an escape)
and, considering Tarzan's ambitions to expand its business to a network, Tarzan and Jane
decide to use a public key system and both install CT in their computers.

The first thing that Tarzan needs to do is to generate a key-pair, this is a private and
public key so that Jane (as well as other future partners) can send him files and
messages. 

To do this Tarzan opens a console (Windows+R in windows, Control+alt+t in unix/linux,
Command+space+"terminal" in MAC) and types the following command:

    crypthru create-key-pair

Here is a summary of Tarzan's session:

    You are going to generate a new ley-pair. Please answer the following questions.
    
    What is your ID (email, phone, code name... ) (\q to cancel): tarzan@jungle.com
    
    Type the passphrase for tarzan@jungle.com (\q to cancel):
    
    Confirm (type again) your passphrase (\q to cancel):
    
    Your public key tarzan@jungle.com has been generated into file /home/tarzan/.crypthru/public/tarzan@jungle.com.key.
    Never mind in protecting or hiding this file. You can publicly transmit to
    whoever will send you messages or data.
    
    
    Your private tarzan@jungle.com key has been generated into file /home/tarzan/.crypthru/private/tarzan@jungle.com.key.
    .------------------------------------------------------------------------.
    | Please keep this file STRICTLY WITH YOU and never transmit to anybody! |
     ------------------------------------------------------------------------ 

Obviously Tarzan's passphrase is not visible and not shown!

Tarzan will take care of keeping the file with its private key (
/home/tarzan/.crypthru/**private**/tarzan@jungle.com.key) absolutely in his own hands (there are many
ways to do this) and send the public key (/home/tarzan/.crypthru/public/tarzan@jungle.com.key)  to 
Jane and whoever else will communicate to him. No special provisions needed for the second file,
no issue if anybody gets it, so Tarzan will send the key through normal email.

Jane has already prepared the file `monkey-work.xlsx` with the weekly performance work
of the monkeys. Upon receiving Tarzan's public key will issue the following command:

    crypthru encrypt path=monkey-work.xlsx public-key=tarzan@jungle.com.key

This will produce the file `monkey-work.xlsx.pgp` that only Tarzan can decrypt. Notice that 
the file name is the same name of plain-text file (so is called the file before encrypring,
no matter if it contains text) suffixed by `.pgp`.

Jane now sends `monkey-work.xlsx.pgp` to Tarzan, again as an attachment to a mail
message, no special provisions since only Tarzan can decrypt the file.

Upon receiving the encrypted file, Tarzan issues the command:

    crypthru decrypt path=monkey-work.xlsx.pgp

CT will ask Tarzan its passphrase (the one he gave and confirmed when he generated the key-pair)
and will restore the file `monkey-work.pgp` prepared by Jane.

How did CT know which private key to use? Simple: it used the only private key in Tarzan's keystore.
Tarzan could also indicate the file containing the private key, or an ID for the private
key (CT has many options and alternatives). In the same way Jane could have imported
Tarzan's key in her own keystore and simply indicate its ID instead of a file. CT can even
lookup in the history of old private keys, which one to use based on the date of the encrypted
file. 

CT can also actively monitor directories (folders) and automatically encrypt files that are
copied or dropped into them, delete the plain text file and send the encrypted files to
destination. CT is flexible and smart to adapt to most different workflow needs.  

## How CT works

CT executes a sequence of **Directives** specified as arguments or written in plain text files.

In previous examples all arguments were on a command line, but they can be also
coded in [Yaml](https://en.wikipedia.org/wiki/YAML) files. For example:

    crypthru encrypt monkey-work.xlsx -public-key tarzan@jungle.com.key

can also be written as:

    crypthru -run my-script.yml

with `my-script.yml` containing:

    - directive: encrypt
      path: monkey-work.xlsx
      public-key: tarzan@jungle.com.key

You can also specify a set of switches and options on the command line.

To list the full list of options and switches use:

    crypthru -help

To print a copy of this guide use:

    crypthru -guide

Each directive is described in later sections. Keep in mind that when you enter a directive
on the command line (instead of the YML file) the first argument is de directive name, followed
by its parameters where the YML semicolon has been replaced by an equal sign (`=`). 

## The Keystore

So each party within the organization, is uniquely identified by an id that is usually its email,
phone number or any kind of code name or surrogate ID that you adopt.

CT can use a keystore based on the file system. Such keystore is usually `~/.crypthru` where `~` 
is your home directory. You can use an alternative location by specifying the `-ks` switch
on the command line.

Here is a sample content of a keystore:

  - .crypthru 
    - keys (directory)
      - private (directory)
        - yourname@mycompany.key
      - public (directory)
        - ann@mycompany.com.key
        - accounting@mycompany.com.key
        - jungle (directory)
          - tarzan@mycompany.com.key
          - jane@mycompany.com.key
        ...

This is: the "keys/private" subdirectory contains your private keys (usually one),
and the "keys/public" contains the public keys. Each key is the party ID suffixed
by ".key": simple, crystal clear and easy to inspect!

Using this keystore you can reference parties by the actual file path (public/private-key)
or ID (public-private-ID). Whenever you specify the ID, CT will search the matching keystore
entry.

## Key-lists

Did you notice the `jungle` directory? CT let you also create
**key-list** directories that are normal directories (under keys/public directory) in which
you can copy one or more public keys. Encrypting a file for `jungle` would
encrypt it for Tarzan, Jane and all other members of the list. 

Whenever ever new monkeys join the list, simply copy their public key in the directory. 
When they leave (cause somebody shuts down an annoying screaming monkey), just
delete the key file.

## Key-history

When you generate a new key-pair or import a public key, and a key with the same ID exists,
CT saves it inserting the [Unix time](https://en.wikipedia.org/wiki/Unix_time), for 
example in `tarzan@jungle.com~1616512491.key` the UNIX time is 1616512491 (seconds since)
epoch. 

This is important since, if you generate a new key-pair (for example because you suspect
that your old private key has been stolen), you will be unable to open decrypt old
encrypted files with the new key. Keeping the history will let you choose which key
to use at any time.

## Default private key

Supposing that Tarzan has multiple private keys (from time to time he works as Zorro too)
asking him to supply each time its private ID or key file path would be tedious, and
irritating Tarzan can be dangerous. So CT uses the following algorithm to guess the private
key to use:

If you specify a private key id (-private-id switch), them  this one is used, otherwise if there is
a single private key this key will be used.
   
## Daemon Execution

In addition to scanning directories for encryption and decryption, CT can operate
as a daemon. If you specify the `-watch` switch on command line, then CT will stay 
active and watch the directories. New files copied or dropped will be subject to
the same encrypt and decrypt rules.

## Directives

The following sections describe in detail each available directive.

---


