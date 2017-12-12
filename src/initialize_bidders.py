import os
import sys

def compile(filename):
    cmd = 'javac ' + filename
    os.system(cmd)

def execute(filename, port_no):
    cmd = 'java ' + filename[:-5] + ' ' + str(port_no)
    os.system("start /B start cmd.exe @cmd /k" + cmd)


def main():
    if len(sys.argv) < 2:
        print("Enter number of bidders : ")
        bidders = int(input())

    else:
        bidders = int(sys.argv[1])

    filename = "Bidder.java"
    compile(filename)
    port_no = 10000

    for i in range(bidders):
        execute(filename, port_no)
        port_no += 1

main()



