Encrypt a set of files in a directory.

The directory is scanned for any matching a set of predefined patterns.
patterns. If the file matches and there is no encrypted file, or
the encrypted file is older than the plain text file then encrypt
will take place.

Encrypted files are suffixed by `.pgp` ot `.gpg`. Thus, for example,
`spreadsheet.xmlx` will be encrypted into `spreadsheet.xmlx.pgp` and
`somedata` into `somedata.pgp`. 

Example:

    - directive: encrypt
      path: ~/myfolder/outbox
      filter:
        - exclude: '*.bak.*'
        - include: '*.txt'
        - include: '*.xlsx'
      privateId:
        - user1@domain.com
        - user2@domain.com
        - some-mailing-list

Please notice that the special character `~` will be replaced with your home directory.

CT also replaces environment and system variables in whatever text in, so, if you define
your directives.

    WHERE=myfolder

You could have written:

      path: ~/${WHERE}/outbox

### Parameters

path
:  The directory or file path to scan. You can indicate a generic
path for the file that will act as first inclusion pattern (see below),
for example `myfolder/outbox/*.png`, in this case `*.png` will become first
inclusion pattern.

filter
:   One or more pattern to be tested (sequentially) for inclusion or exclusion
of each file in the process. The first matching pattern will determine if a
file will be included or excluded. If no pattern match, and a generic path
was indicated, the file will be ignored (not included) but if the path was
a directory the file will be included.

public-key
:   One or more public key files to be used (in addition top publicKeyID)

public-id
:  One or more public key IDs to be used (in addition top publicKeyFile)

meToo
:   `true` if you want to include your public ID in the list. Default is `false`.
If you do not specify meToo, and you wipe the encrypted files you will be
unable to restore them. Only the public keys / IDs parties could do that.

wipe
:  Upon successful encryption the plain text files will be deleted.

zip
:  An archive name to be generated. The archive name will be automatically
suffixed by `.zip` (if needed) and will contain all eligible files. So if,
for example, you specify `my-bundle`, this will result in the file 
`my-bundle.zip.pgp` that, upon decrypting will generate `my-bundle.zip`
contain (in plain text) all selected files.

force
: `true` to force encryption even if the .pgp file exists and its date
follows last plain text file update. 
