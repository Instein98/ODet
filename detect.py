import sys
import time
import json
from pathlib import Path
import subprocess as sp
import xml.etree.ElementTree as et
# execute `mvn test` with the javaagent

# analyze the json files

workDirPath = None
odetDirPath = None
javaagentPath = "/Users/yicheng/.m2/repository/edu/illinois/odet-javaagent/1.0-SNAPSHOT/odet-javaagent-1.0-SNAPSHOT.jar"
executionConfigFileName = 'tmpConfig.json'
resultFileName = 'result.json'

def executeDetection(config: dict, instPrefix: str, origPassedTestList, origFailedList):
    """
    Execute the detection phase and return polluter-victim pairs
    """
    res = []  # real victim tests
    if len(config['tests']) == 0:
        return res
    generateTmpConfig(config)

    print('polluter: ' + config['polluter'])
    print('tests: ' + str(config['tests']))
    print()

    sp.run('mvn test -DargLine="-javaagent:{}=instPrefix={};mode=detect;detectConfig={}" -Dtest={} -Dmaven.test.failure.ignore=true'.format(javaagentPath, instPrefix, executionConfigFileName, ','.join(config['tests'])).split(), shell=False, universal_newlines=True, cwd=str(workDirPath))

    passedTestList, failedTestList = analyzeMvnTestResult(workDirPath)

    for test in passedTestList:
        if test in origFailedList:
            res.append(test)
    for test in failedTestList:
        if test in origPassedTestList:
            res.append(test)

    return res
    # print('mvn test -DargLine="-javaagent:{}=instPrefix={};mode=detect;detectConfig={}" -Dtest={} -Dmaven.test.failure.ignore=true'.format(javaagentPath, instPrefix, executionConfigFileName, ','.join(config['tests'])))
    # exit(0)

    # print()
    
def analyzeTestResult():
    """
    Todo: analyze test execution result
    """
    return None

def generateTmpConfig(config: dict):
    targetPath = odetDirPath / executionConfigFileName
    with targetPath.open(mode='w') as f:
        json.dump(config, f)

def analyzeMvnTestResult(workingDir: Path):
    """
    Parse the surefire test results from the target/surefire-reports directory
    """
    successList = []
    failedList = []
    reportsDir = workingDir / 'target/surefire-reports'
    assert reportsDir.exists() and reportsDir.is_dir()
    for file in reportsDir.iterdir():
        if file.name.endswith('.xml') and file.name.startswith('TEST'):
            tree = et.parse(str(file))
            root = tree.getroot()
            tests = root.findall("./testcase")
            for testcase in tests:
                className = testcase.attrib['classname']
                testName = testcase.attrib['name']
                testId = '{}#{}'.format(className, testName)
                if len(list(testcase)) == 0:
                    successList.append(testId)
                else:
                    failure = testcase.find("./failure")
                    error = testcase.find("./error")
                    if failure is not None or error is not None:
                        failedList.append(testId)
    return successList, failedList

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
    
    # analyze the current test results
    origPassedTestList, origFailedTestList = analyzeMvnTestResult(workDirPath)

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
    
    pretty(executionDict)

    # start execution to detect victims

    executionIteration = 0
    executedTestsNum = 0
    resultDict = {}
    executionStartTime = time.time()

    for polluterTest in executionDict:
        # print('======= ' + polluterTest)
        ## generate configuration file for the javaagent about what states to reset, what tests to run
        ### prepare states that need to be deserialized
        deserializationDict = polInfo[polluterTest]
        ### If some tests need to run are polluters themselves, we need to isolate them
        testsToRun = executionDict[polluterTest]
        cleanTestsToRun = [test for test in testsToRun if test not in potentialPolluters]
        polluterTestsToRun = [test for test in testsToRun if test in potentialPolluters]

        # print('cleanTestsToRun = ' + str(cleanTestsToRun))
        # print('polluterTestsToRun = ' + str(polluterTestsToRun))

        ### execute non-potential-polluter tests
        config = {}
        config['polluter'] = polluterTest
        config['states'] = deserializationDict
        config['tests'] = cleanTestsToRun

        realVictims = []
        if len(cleanTestsToRun) > 0:
            print('Executing: {} - {}'.format(polluterTest, str(cleanTestsToRun)))
            realVictims.extend(executeDetection(config, instPrefix, origPassedTestList, origFailedTestList))
            executionIteration += 1
            executedTestsNum += len(cleanTestsToRun)

        ### execute potential polluter tests individually
        for test in polluterTestsToRun:
            config['tests'] = [test]
            print('Executing: {} - {}'.format(polluterTest, str(test)))
            realVictims.extend(executeDetection(config, instPrefix, origPassedTestList, origFailedTestList))
            executionIteration += 1
            executedTestsNum += 1

        if len(realVictims) > 0:
            resultDict[polluterTest] = {}
            resultDict[polluterTest]['states'] = deserializationDict
            resultDict[polluterTest]['victims'] = realVictims

    executionTimeCost = time.time() - executionStartTime

    print('='*50)
    print('# Execution Iteration: {}'.format(executionIteration))
    print('# Executed Tests: {}'.format(executedTestsNum))
    print('Execution Cost: {}'.format(executionTimeCost))
    print('='*50)
    for polluter in resultDict:
        print('Polluter: {}'.format(polluter))
        pretty(resultDict[polluter])
    print('='*50)
    # dump result
    outputPath = odetDirPath / resultFileName
    with outputPath.open(mode='w') as f:
        json.dump(resultDict, f)

def pretty(d, indent=2):
   for key, value in d.items():
      print(' ' * indent + str(key))
      if isinstance(value, dict):
         pretty(value, indent+1)
      else:
         print(' ' * (indent+1) + str(value))

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Need 2 arguments as the path of working directory and instPrefix!')
        exit(1)
    workDirPath = Path(sys.argv[1])
    instPrefix = Path(sys.argv[2])
    odetDirPath = workDirPath / 'odet/'
    main(instPrefix)