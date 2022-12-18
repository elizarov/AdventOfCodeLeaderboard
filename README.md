# Advent of Code private leaderboards downloader and aggregator

#### First time before use

`npm install`

#### Download all private leaderboards

`npm start`
* First time: login into your account and navigate to 'Private Leaderboards' page.
  * Cookies are automatically saved to `cookies.json` for next launches.
* All private leaderboards are saved to `json` directory.


#### Print the combined leaderboard

`gradlew run`
* Add `-PexcludeIds=<comma-separated-list>` to ignore some leaderboards.
* Add `-PdaysRange=<day>|<day1>..<day2>` to specify a range of days to show (defaults to 1..25).