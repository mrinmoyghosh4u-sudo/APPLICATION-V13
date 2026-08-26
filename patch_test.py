with open("app/src/test/java/com/example/Phase1ProviderHealthTest.kt", "r") as f:
    content = f.read()

content = content.replace("assertEquals(\"LIVE\", state.status)", "assertEquals(\"LIVE\", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).status)")

with open("app/src/test/java/com/example/Phase1ProviderHealthTest.kt", "w") as f:
    f.write(content)
