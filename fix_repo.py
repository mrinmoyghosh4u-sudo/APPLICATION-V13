content = open('app/src/main/java/com/example/data/repository/TradingRepository.kt').read()

content = content.replace("dao.getWatchlist().firstOrNull()", 'dao.getWatchlist("ALL").firstOrNull()')
content = content.replace("dao.insertWatchlistItem(idx)", 'dao.addWatchlistItem(idx)')
content = content.replace("dao.insertWatchlistItem(it)", 'dao.addWatchlistItem(it)')

open('app/src/main/java/com/example/data/repository/TradingRepository.kt', 'w').write(content)
