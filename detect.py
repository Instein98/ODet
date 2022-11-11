import sys
import json
from pathlib import Path
# execute `mvn test` with the javaagent

# analyze the json files

def main(workDir: str):
    workDirPath = Path(workDir)
    odetDir = workDirPath / 'odet'
    assert odetDir.exists()
    accJson = odetDir / 'accessInfo.json'
    polJson = odetDir / 'pollutionInfo.json'
    assert accJson.exists() and polJson.exists()

    # load json files
    with accJson.open() as f:
        accInfo = json.load(f)
    with polJson.open() as f:
        polInfo = json.load(f)
    
    # print(accInfo)
    # print(polInfo)

    # analyze the pollution that need to be reset and the tests need to be run
    executionDict = {}
    # for each potential polluters, find the potential victims to run
    for polluterTest in polInfo:
        executionDict[polluterTest] = []
        for pollutedField in polInfo[polluterTest]:
            # find the tests accessing this field
            for testId in accInfo:
                if pollutedField in accInfo[testId] and testId != polluterTest:
                    executionDict[polluterTest].append(testId)
    
    print(executionDict)
    # start execution to detect victims
    

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print('Need a argument as the path of working directory!')
        exit(1)
    workDir = sys.argv[1]
    main(workDir)