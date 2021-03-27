Executes a system command. This can be useful to perform generic tasks
(eliminating old files and so on), comparing decrypted files with the
original and so on.

Example (yml - remove all pgp files from `mydir` directory):

    - directive: execute
      command: rm mydir/*.pgp

### Parameters

command
:    The commands to be executed. If the command spans multiple lines each
line is executed in sequence as a single command.

quiet
:    Do not display output (this is the default).

lenient
:    Proceed even independently of return code.  