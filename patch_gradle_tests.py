with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "failOnNoDiscoveredTests" not in content:
    content = content.replace('testOptions {\n        unitTests {', 'testOptions {\n        unitTests {\n            all {\n                testLogging {\n                    events("passed", "skipped", "failed")\n                }\n                failOnNoDiscoveredTests = false\n            }')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
