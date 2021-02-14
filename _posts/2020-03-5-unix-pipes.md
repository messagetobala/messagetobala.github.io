---
layout: post
title:  "Understanding pipes in Unix with a sample implementation"
date:   2020-03-05 13:19:16 +0530
categories: unix
excerpt: Explains how command pipeline works in Unix. We will look at the concepts behind it and also write our own implementation.
permalink: /unix-pipes.html
comments_id: 1
---
[Do one thing well](#do-one-thing-well)

[Everything is a file](#everything-is-a-file)

[‘Pipe’ a way to pass on information](#pipe-a-way-to-pass-on-information)

[Replacing the output and input descriptors](#replacing-the-output-and-input-descriptors)

[Changing the code of a running process](#changing-the-code-of-a-running-process)

[The last step](#the-last-step)


## Do one thing well ##

“***Do one thing well***” is one of the underlying design principles of  Unix operating system. Any one with a little experience  in working with Unix and Unix like systems, would have seen the benefits of  this simple principle,  when they combine different programs  using pipe (‘\|’) in a shell, to accomplish a specific requirement.

For example, lets say we have a log file called ‘application.log’  where for every request received by an application, there is a log line like ‘Request  from user1’.  Now if we want to find the users who are sending the most number of requests, we could run the following command.


    #>cat application.log | grep 'Request from ' | sort | uniq -c | sort -bgr'

The above command would give  me the count of requests received from each user , sorted by the count in the descending order.

As you can see, using  four different programs each designed to do a particular thing, we have accomplished our requirement.   Every program uses the output of previous program as its input, perform that one thing it is designed to do and passes on the output to the next program. 

***But how is this implemented and what exactly happens when we run a command like this ?*** In this article, I will try to explain this, taking you step by step through every concept involved.

# Everything is a file

In Unix everything is considered as a file.  Whether we are reading/writing from/to a file, device or sockets, they are all considered as a file.  Every process has a  data structure called ‘ ***File Descriptor Table*** ’ , which would have an entry for each file used by the process.  You could think of it as Hash Table where the key is an integer called the ‘ ***File Descriptor*** ’ and the value is a pointer to an  entry in another data structure called the ‘ ***File Table*** ’.   ‘File Table’ contains information about the file like the current offset etc.  Every process by default will have three entries in the ‘File Descriptor Table’ for keys 0,1,2 which represent the standard input, standard output and standard error.


![](https://paper-attachments.dropbox.com/s_40A490A0758DF9E1DC078DAB5932FC2B373486202053BADFD85DB4A709379186_1585396525514_pipe1.png)


The key thing to understand is that the ‘File Descriptor Table’ is per process but the ‘File Table’ is shared across all processes.  This brings us to a question. ***When a process uses fork and creates a child process, how will the ‘File Descriptor Table’ of the child process look like?***

Take a look at the following program and the sample output.

***pipe_1.c***
```c
    #include <stdio.h>
    #include <stdlib.h>
    #include <fcntl.h>
    #include <unistd.h>
    #include <sys/wait.h>
    #include <string.h>
    #include <errno.h>
    #include <stdlib.h>
    
    const char* process = NULL;
    void read_from_fd(int fd) {
        char c;
        while(read(fd, &c, 1) != 0) {
           printf("Process [%s] Read Reading from descriptor %d character %c\n", process, c);
           sleep(1);
        }
    }
    int main(int argc, char** argv) {
        process = "PARENT"; 
        int read_fd = open("/tmp/file1", O_RDONLY);
        char c;
        read(read_fd, &c, 1);
        printf("Process [%s] Read character %c\n", process, c);
        printf("Forking a new child process \n");
        int pid = fork();
        if (pid == -1) {
                fprintf(stderr, "Erroring creating child process. %s", strerror(errno));
                exit(-1);
        } else if (pid == 0) {
                process = "CHILD";
                read_from_fd(read_fd);
                exit(0);
        } else {
                read_from_fd(read_fd);
                waitpid(pid, 0, 0);
                exit(0);                                
        } 
    }
```

***Sample Output***                                                            
```bash
    #>echo -n "ABCDEFGHIJKLMNOPQRSTUVWXYZ" > /tmp/file1 
    #>gcc pipe_0.c 
    #>./a.out 
    Process [PARENT] Read character A
    Forking a new child process 
    Process [PARENT] Reading from descriptor 3 character B
    Process [CHILD] Reading from descriptor 3 character C
    Process [CHILD] Reading from descriptor 3 character D
    Process [PARENT] Reading from descriptor 3 character E
    Process [CHILD] Reading from descriptor 3 character F
```
                                                       
The program, opens a file for reading, reads a single character and then calls fork to create a child process.   After fork, both the parent and the child process continue to read from the same file descriptor.    

From the output, we can see that the child process is able to read from the same file descriptor“3” (here 3 is the file descriptor of file ‘/tmp/file1’) as the parent. This is because the child process gets an exact copy of the “File Descriptor Table” from the parent at time of calling fork. See the image below.

Also note that every character is printed only once. This is because the entry for file descriptor “3” in both the parent and child process points to the same entry in the “File Table” and thus they share the same “current offset”. So even though the parent and child process are reading the same file, we don’t see the same character printed twice.


![](https://paper-attachments.dropbox.com/s_40A490A0758DF9E1DC078DAB5932FC2B373486202053BADFD85DB4A709379186_1585397250445_image2.png)

## ‘Pipe’ a way to pass on information ##

A ‘Pipe’ is a construct in the Unix operating system that provides a way for communication between two processes. It could be considered analogous to the ‘Water Pipes’ we see in our household. Just like the ‘Water Pipes’ it has two ends. One is called the ‘Read End’ and another is called the ‘Write End’.   Any thing written in the ‘Write End’ would be available to be read from the ‘Read End’.  These two ends are represented by two file descriptors which are stored in the ‘File Descriptor Table’

A ‘Pipe’  is created using the ‘pipe’ function. The input to the function is an integer array of size 2. On successful return from the ‘pipe’ function, the integer array would be filled with the two file descriptors that represents the two ends of the pipe. The descriptor at index ’0’ represents the read end of the pipe and the descriptor at index ’1’ represents the write end of the pipe.

***Creating a pipe***
```c
      int fd[2];
      if (pipe(fd) != 0) {
          printf ("Error creating pipe. %s", strerror(errno));
          exit(errno);
      }
```
                                                        

In the earlier section, we saw that , when a child process is forked it gets an exact copy of the parent’s ‘File Descriptor Table’.   So, if a process creates a pipe and then forks a child process, 
the ‘File Descriptor Table’ of the child process will have a copy of the two descriptors that represent the two ends of the pipe.  ***So if the parent process writes data to fd[1], could it be read from the fd[0] in the child process? .***   Let us implement it and see.  

The program below creates a pipe and then calls fork to create a child process.  After fork, In the parent process we write data to the write end (fd[1]) of the pipe .  In the child process we try to read data from the read end (fd[0]) of the pipe.

***pipe_2.c***
```c
    #include <unistd.h>
    #include <stdio.h>
    #include <string.h>
    #include <errno.h>
    #include <sys/wait.h>
    #include <stdlib.h>
    const char* process = NULL;
    void read_from_fd(int fd) {
        char c;
        printf ("Process [%s] Reading from descriptor %d \n", process, fd);
        while(read(fd, &c, 1) != 0) {
          printf("%c", c);
        }
    }
    int main(int argc, char** argv) {
        process = "PARENT";
        int fd[2];
        if (pipe(fd) != 0) {
          printf ("Error creating pipe. %s", strerror(errno));
          exit(errno);
        }
        int pid = fork();
        if (pid == -1) {
            printf ("Error creating pipe. %s", strerror(errno));
            _exit(errno);
        } else if (pid == 0) {
            //child        
            process = "CHILD";
            /* Close the write end of the pipe in child process.It is not used */
            close(fd[1]);  
            read_from_fd(fd[0]);
            exit(0);
        } else {
            /* Close the read end of the pipe in parent process.It is not used */
            close(fd[0]); 
            const char* data = "Data";
            printf("Process [%s] Writing data to pipe\n", process);        
            write(fd[1], data, strlen(data));
            /* After writing all data, close the write end */
            close(fd[1]);
            /* Wait for the child to finish */
            waitpid(pid, 0, 0);
            exit(0);                        
        }
    }
```
                                                           
***Sample Output***                                                         
```bash
    #>gcc pipe_1.c 
    #>./a.out 
    Process [PARENT] Writing data to pipe
    Process [CHILD] Reading from descriptor 3 
    Data
```
                                                             

As can be seen from the sample output, the child process is able to read data from the pipe that parent process wrote into the pipe.  Thus ‘Pipe’ provides us a way to pass on information from a parent process to child process.


## Replacing the output and input descriptors ##

In the previous program we were able to pass information from one process to another process using the  write/read descriptors of the pipe. But for our use case, we want a process to write data to its standard output and it should be readable from the standard input of another process.  That is, in the previous program the parent process instead of writing into fd[1] should write into descriptor ’1’ (Which represents standard output). And  the child process instead of reading from fd[0] should read from descriptor ’0’ (Which represents the standard input).

This could be achieved through the ‘dup2’ function.  The ‘dup2’ function accepts two integer arguments.  The first argument represents a descriptor that already exists in the ‘File Descriptor Table’.    If the second argument does not represent an already existing descriptor , a new entry is created in the ‘File Descriptor Table’  and the value corresponding to the first descriptor is copied into the new row.  If the second argument represents an existing descriptor, a close function is called for that descriptor and the older value is replaced with the value corresponding to the first descriptor.

So, using the ‘dup2’ function we can copy the write descriptor of the pipe into descriptor ’1’ which represents the standard output.  Similarly in the child process, we can copy the read descriptor of the pipe into descriptor ’0’ which represents the standard input.

The implementation and sample output is shown below.

***pipe_3.c***
```
    #include <unistd.h>
    #include <stdio.h>
    #include <string.h>
    #include <errno.h>
    #include <sys/wait.h>
    #include <stdlib.h>
    const char* process = NULL;
    void read_from_fd(int fd) {
        char c;
        printf ("Process [%s] Reading from descriptor %d \n", process, fd);
        while(read(fd, &c, 1) != 0) {
                printf("%c", c);
        }
    }
    
    int main(int argc, char** argv) {
        process = "PARENT";
        int fd[2];
        if (pipe(fd) != 0) {
            printf ("Error creating pipe. %s", strerror(errno));
            exit(errno);
        }
    
        int pid = fork();        
        if (pid == -1) {
            printf ("Error creating pipe. %s", strerror(errno));
            exit(errno);
        } else if (pid == 0) {
            //child        
            process = "CHILD"; 
            /*Replace standard input of child process with read end of the pipe*/       
            dup2(fd[0], 0);
            /* Close the write end of the pipe in child process.It is not used */
            close(fd[1]);
            read_from_fd(0);
            exit(0);
        } else {
            const char* data = "Data";          
            /*Replace standard output of parent process with write end of the pipe*/
            dup2(fd[1], 1);
            /* Close the read end of the pipe in parent process.It is not used */
            close(fd[0]);
            write(1, data, strlen(data));
            /* After writing all data, close the write end */
            close(fd[1]);
            close(1);
            /* Wait for the child to finish */
            waitpid(pid, 0, 0);
            exit(0);                        
        }
    }
```
                                                           
***Sample Output***                                                            
```bash
    #> gcc pipe_3.c 
    #>./a.out 
    Process [CHILD] Reading from descriptor 0 
    Data
```    
                                              


## Changing the code of a running process ##

In our previous program, the parent process writes to it is standard output and the child process reads the same information from its standard input. This is exactly what happens when we run a command like “cat file1 \| wc -l” in Unix shell. 

But instead of just writing some fixed data into standard output, we want our parent process to run another program (like ‘cat’ in the example) .  Similarly in our child process instead of just reading from standard input and printing it to standard output, we want to run another program that will process the input (like ‘wc’ in the example).

This is achieved through the ‘exec’ family of functions. What these functions do is they replace the text, code,stack segments of a running process with that of a another program.  The ‘File Descriptor Table’ though would still remain the same. 

So we can modify the previous program such that after fork, in the parent process we run the ‘cat’ program which will read the contents of the given file and write it to standard output which is pointing to the write end of the pipe.   Similarly in the child process, we can run the ‘wc’ program which will read from the standard input which would be the read end of the pipe and write the output to its standard output.

See below implementation and sample output.

***pipe_4.c***
```c
    #include <unistd.h>
    #include <stdio.h>
    #include <string.h>
    #include <errno.h>
    #include <sys/wait.h>
    #include <stdlib.h>
    int main(int argc, char** argv) {
        int fd[2];
        if (pipe(fd) != 0) {
            printf ("Error creating pipe. %s", strerror(errno));
            exit(errno);
        }
        int pid = fork();
        if (pid == -1) {
            printf ("Error creating pipe. %s", strerror(errno));
            exit(errno);
        } else if (pid == 0) {
            //child        
            /* Replace standard output of child process with read end of the pipe */
            dup2(fd[0], 0);
            /* Close the write end of the pipe in child process.It is not used */
            close(fd[1]);
            execlp("wc", "wc", "-c", NULL);
        } else {
            //parent
            /* Replace standard output of parent process with write end of the pipe */
            dup2(fd[1], 1);
            /* Close the read end of the pipe in parent process.It is not used */
            close(fd[0]);
            execlp("cat", "cat" , "/tmp/file1" , NULL);        
        }
    }
```  

                                                                
***Sample Output***                                                                
```bash
    #> gcc pipe_4.c 
    #> cat /tmp/file1
    ABCDEF
    #>./a.out 
           6
```
                                                             
                                                                     

## The last step ##

The previous program almost mimics the behavior of a Unix shell in executing commands chained using a pipe. But it chains together only two commands and also the commands and the arguments are hard coded.

The below program is more closer to the shell. It accepts as input ,  the command chain to execute. For each command, it forks a new process, executes the command and passes on the output to the next command.   It still lacks few features like handling input with characters like ‘”,\\,\|’ .  I will leave that as an exercise .  Hope this was useful.

***pipe.c***
```c
    #include <unistd.h>
    #include <stdio.h>
    #include <string.h>
    #include <errno.h>
    #include <sys/wait.h>
    #include <stdlib.h>
    
    
    void execute_command(char* command) {
        int i = 0;
        int argument_count = 0;
    
        /* Strip white spaces */
        while (command[i] == ' ') {
          i++;
        }
        command = command + i;
    
        i = 0;
        /* Count the number of arguments to the command */
        while (command[i] != '\0') {
            if (command[i] == ' ')
                    argument_count++;
            i++;
        }
        char** argv = calloc(argument_count + 2, sizeof(char*));
        char* argument = NULL;
        i = 0;        
        while ((argument = strsep(&command, " ")) != NULL) {
            if (strlen(argument) != 0) {
                argv[i] = calloc(strlen(argument) + 1, sizeof(char));
                strncpy(argv[i], argument, strlen(argument));
            }
            i++; 
        }
        /* Need to set the last argument as NULL */ 
            argv[i] = NULL;
            
            if (execvp(argv[0], argv) != 0) {
               fprintf(stderr, "Error creating pipe. %s", strerror(errno));
            }
    }
    
    int main(int argc, char** argv) {
        if (argc != 2) {
            printf ("Usage pipe <commands to execute>");
            exit(-1);
        }
        int* fd = calloc(2, sizeof(int));      
        if (pipe(fd) != 0) {
            printf ("Error creating pipe. %s", strerror(errno));
            exit(errno);
        }
        const char* command = argv[1];
        int prev_commands_length = 0;
        int i = 0;
        int quote_begin = 0;
        while (1) {
            if (command[i] == '|') {
                /*  End of a command */
                int pid = fork();
                if (pid == -1) {
                    printf("Error creating pipe. %s", strerror(errno));
                    exit(errno);
                } else if (pid > 0) {
                    /*
                      Parent will take care of command seen.
                      And send its output to child.
                     */
                    dup2(fd[1], 1);
                    close(fd[0]);        
                    char* current_command = calloc(i + 1 - prev_commands_length, sizeof(char));
                    strncpy(current_command, command + prev_commands_length, i - prev_commands_length);
                    execute_command(current_command);
                    } else {
                       dup2(fd[0], 0);
                       close(fd[1]);
                       /* Create new pipe for chaining the next two commands */
                       fd = calloc(2, sizeof(int));
                       pipe(fd);
                    }
                    prev_commands_length = i + 1;
            } else if (command[i] == '\0') {
                char* current_command = calloc(i + 1 - prev_commands_length, sizeof(char));
                strncpy(current_command, command + prev_commands_length, i - prev_commands_length);
                execute_command(current_command);
            }

            i++;                
        }
    }
```                                                                  

***Sample Output***
```bash
    #> gcc pipe.c
    #> cat /tmp/file2 
    apple
    orange
    apple
    mango
    apple
    orange
    guava
    kiwi
    #>./a.out 'cat /tmp/file2 | sort | uniq -c | sort -bgr'
       3 apple
       2 orange
       1 mango
       1 kiwi
       1 guava
```
