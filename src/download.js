const { chromium } = require('playwright');
const fs = require('fs');

const year = "2022"
const siteRoot = "https://adventofcode.com"
const leaderboardsPage = siteRoot + "/" + year + "/leaderboard/private"
const jsonDir = "json"
const cookiesFile = "cookies.json"

const run = async () => {
    const browser = await chromium.launch({ headless: false });
    const context = await browser.newContext(
        fs.existsSync(cookiesFile) ? { storageState: cookiesFile } : {}
    );
    const page = await context.newPage();
    await page.goto(leaderboardsPage);
    if (page.url() != leaderboardsPage) {
        console.log("!!! PLEASE LOGIN AND GO TO PRIVATE LEADERBOARDS !!!");
        await page.waitForNavigation({ url: leaderboardsPage, timeout: 600000 });
        await page.context().storageState({ path: cookiesFile });
        console.log(`Saved authentication state to '${cookiesFile}'`);
    }
    if (!fs.existsSync(jsonDir)){
        fs.mkdirSync(jsonDir);
    }
    const list = page.locator('a:text("[View]")');
    const count = await list.count();
    let urls = [];
    for (var i = 0; i < count; i++) {
        const element = await list.nth(i);
        const href = await element.getAttribute('href');
        urls.push(href + '.json');
    }
    for (const i in urls) {
        const url = urls[i];
        const file = jsonDir + url.substring(url.lastIndexOf('/'));
        console.log(`Saving leaderboard ${url} -> ${file}`);
        await page.goto(siteRoot + url);
        const content = await page.innerText('pre');
        fs.writeFile(file, content, err => {
            if (err) console.error(err);
        });
    }
    await browser.close();
};

run();
