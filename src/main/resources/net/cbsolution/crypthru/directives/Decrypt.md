Decrypt the encrypted files in a directory.

The directory is scanned for any encrypted file. Encrypted files are
suffixed by `.pgp` ot `.gpg`. When one of such files is found, and
the corresponding plain text file (removing `.pgp` ot `.gpg`) does
not exist, or it is older than the encrypted file, decryption takes
place generating the corresponding decrypted file.

Please refer to the Keystore section to understand which private
key is used.

Example (yml):

    - directive: decrypt
      directory: ~/myfolder/inbox

Please notice that the special character `~` will be replaced with your home directory.

CT also replaces environment and system variables in whatever text in, so, if you define
your directives.

    WHERE=myfolder

You could have written:

      path: ~/${WHERE}/inbox


### Parameters

path
:  The directory or file path to scan. You can indicate a generic
path for the file that will act as first inclusion pattern (see below),
for example `myfolder/outbox/*.png.pgp`, in this case `*.png.pgp` will become first
inclusion pattern.

filter
:   One or more pattern to be tested (sequentially) for inclusion or exclusion
of each file in the process. The first matching pattern will determine if a
file will be included or excluded. If no pattern match, and a generic path
was indicated, the file will be ignored (not included) but if the path was
a directory the file will be included.

wipe
:  Upon successful encryption the plain text encrypted (.pgp) files will be deleted.

force
: `true` to force decryption even if the plain text file exists and its date
follows last encrypted file update.

unzip
: `true` will automatically extract the content of any decrypted file suffixed
by `.zip` and delete the containing zip-file, automatically preventing 
[zip-slip vulnerability](https://snyk.io/research/zip-slip-vulnerability).
