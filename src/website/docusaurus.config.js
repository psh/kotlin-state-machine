// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
    title: 'Kotlin State Machine',
    tagline: 'State machine with a clean Kotlin DSL',
    url: 'https://your-docusaurus-test-site.com',
    baseUrl: '/',
    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',
    favicon: 'img/favicon.png',

    // GitHub pages deployment config.
    // If you aren't using GitHub pages, you don't need these.
    organizationName: 'psh', // Usually your GitHub org/user name.
    projectName: 'kotlin-state-machine', // Usually your repo name.

    // Even if you don't use internalization, you can use this field to set useful
    // metadata like html lang. For example, if your site is Chinese, you may want
    // to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    presets: [
        [
            'classic',
            /** @type {import('@docusaurus/preset-classic').Options} */
            ({
                blog: false,
                docs: {
                    routeBasePath: '/',
                    sidebarPath: require.resolve('./sidebars.js')
                },
                theme: {
                    customCss: require.resolve('./src/css/custom.css'),
                },
            }),
        ],
    ],

    themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
        ({
            navbar: {
                title: 'Kotlin State Machine',
                logo: {
                    alt: 'Kotlin State Machine Logo Logo',
                    src: 'img/logo.svg',
                },
                items: [
                    {
                        type: 'doc', position: 'left',
                        docId: 'index',
                        label: 'Getting Started',
                    },
                    {
                        type: 'doc', position: 'left',
                        docId: 'observing-state-changes',
                        label: 'Observing State Changes',
                    },
                    {
                        type: 'doc', position: 'left',
                        docId: 'compose',
                        label: 'Compose Support',
                    },
                    {
                        type: 'doc', position: 'left',
                        docId: 'testing',
                        label: 'Testing',
                    },
                    {
                        href: 'https://github.com/psh/kotlin-state-machine',
                        label: 'GitHub',
                        position: 'right',
                    },
                ],
            },
            footer: {
                style: 'dark',
                links: [],
                copyright: `Copyright © ${new Date().getFullYear()} Kotlin State Machine.  Built with Docusaurus.`,
            },
            prism: {
                additionalLanguages: ['kotlin'],
                theme: lightCodeTheme,
                darkTheme: darkCodeTheme,
            },
        }),
};

module.exports = config;
