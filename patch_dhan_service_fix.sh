sed -i 's/class DhanBrokerService(/class DhanBrokerService(\n    override val brokerName: String = "Dhan",\n/' app/src/main/java/com/example/data/network/DhanBrokerService.kt
