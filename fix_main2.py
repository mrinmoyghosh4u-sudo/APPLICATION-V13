content = """                                onDhanLogin = { clientId -> viewModel.connectDhan(clientId) },
                                onUpstoxLogin = { clientId, secret -> viewModel.connectUpstox(clientId, secret) },
                                onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }
                            )
                        }
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/MainActivity.kt", "a") as f:
    f.write(content)
