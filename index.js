/* eslint-disable global-require */
module.exports = {
    get GAMBanner() {
      return require('./RNGAMBanner').default;
    },
    get GAMInterstitial() {
      return require('./RNGAMInterstitial').default;
    },
  };
  