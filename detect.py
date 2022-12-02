import sys
import json
from pathlib import Path
import subprocess as sp
# execute `mvn test` with the javaagent

# analyze the json files

workDirPath = None
odetDirPath = None
javaagentPath = "/Users/yicheng/.m2/repository/edu/illinois/odet-javaagent/1.0-SNAPSHOT/odet-javaagent-1.0-SNAPSHOT.jar"
executionConfigFileName = 'tmpConfig.json'


def executeDetection(config: dict, instPrefix: str, originalTestResultDict: dict):
    """
    Execute the detection phase and return polluter-victim pairs
    """
    res = []
    if len(config['tests']) == 0:
        return res
    generateTmpConfig(config)

    print('polluter: ' + config['polluter'])
    print('tests: ' + str(config['tests']))
    print()

    sp.run('mvn test -DargLine="-javaagent:{}=instPrefix={};mode=detect;detectConfig={}" -Dtest={} -Dmaven.test.failure.ignore=true'.format(javaagentPath, instPrefix, executionConfigFileName, ','.join(config['tests'])).split(), shell=False, universal_newlines=True, cwd=str(workDirPath))

    # print('mvn test -DargLine="-javaagent:{}=instPrefix={};mode=detect;detectConfig={}" -Dtest={} -Dmaven.test.failure.ignore=true'.format(javaagentPath, instPrefix, executionConfigFileName, ','.join(config['tests'])))
    # exit(0)

    print()
    
def analyzeTestResult():
    """
    Todo: analyze test execution result
    """
    return None

def generateTmpConfig(config: dict):
    targetPath = odetDirPath / executionConfigFileName
    with targetPath.open(mode='w') as f:
        json.dump(config, f)

def main(instPrefix: str):
    assert odetDirPath.exists()
    accJson = odetDirPath / 'accessInfo.json'
    polJson = odetDirPath / 'pollutionInfo.json'
    assert accJson.exists() and polJson.exists()

    # load json files
    with accJson.open() as f:
        accInfo = json.load(f)
    with polJson.open() as f:
        polInfo = json.load(f)
    
    # analyze the pollution that need to be reset and the tests need to be run

    executionDict = {}
    potentialPolluters = []
    # for each potential polluters, find the potential victims to run
    for polluterTest in polInfo:
        potentialPolluters.append(polluterTest)
        executionDict[polluterTest] = set()
        for pollutedField in polInfo[polluterTest]:
            # find the tests accessing this field
            for testId in accInfo:
                if pollutedField in accInfo[testId] and testId != polluterTest:
                    executionDict[polluterTest].add(testId)
    
    # start execution to detect victims

    for polluterTestId in executionDict:
        # print('======= ' + polluterTestId)
        ## generate configuration file for the javaagent about what states to reset, what tests to run
        ### prepare states that need to be deserialized
        deserializationDict = polInfo[polluterTestId]
        ### If some tests need to run are polluters themselves, we need to isolate them
        testsToRun = executionDict[polluterTest]
        cleanTestsToRun = [test for test in testsToRun if test not in potentialPolluters]
        polluterTestsToRun = [test for test in testsToRun if test in potentialPolluters]

        # print('cleanTestsToRun = ' + str(cleanTestsToRun))
        # print('polluterTestsToRun = ' + str(polluterTestsToRun))

        ### execute non-potential-polluter tests
        config = {}
        config['polluter'] = polluterTestId
        config['states'] = deserializationDict
        config['tests'] = cleanTestsToRun
        executeDetection(config, instPrefix, analyzeTestResult())

        ### execute potential polluter tests individually
        for test in polluterTestsToRun:
            config['tests'] = [test]
            executeDetection(config, instPrefix, analyzeTestResult())
    

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Need 2 arguments as the path of working directory and instPrefix!')
        exit(1)
    workDirPath = Path(sys.argv[1])
    instPrefix = Path(sys.argv[2])
    odetDirPath = workDirPath / 'odet/'
    main(instPrefix)