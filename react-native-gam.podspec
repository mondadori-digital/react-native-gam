# react-native-gam.podspec

require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-gam"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-gam
                   DESC
  s.homepage     = "https://github.com/mondadori-digital/react-native-gam"
  # brief license entry:
  s.license      = "MIT"
  # optional - use expanded license entry instead:
  # s.license    = { :type => "MIT", :file => "LICENSE" }
  s.authors      = { "Your Name" => "yourname@email.com" }
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/mondadori-digital/react-native-gam.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,c,cc,cpp,m,mm,swift}"
  s.requires_arc = true
  # s.static_framework = true

  # We can't add the Google-Mobile-Ads-SDK as a dependency, as it would prevent
  # this library to be used with `use_frameworks!`.
  # So instead we add the default location of the framework to the framework
  # search paths, and we rely on consumers of this library to add
  # Google-Mobile-Ads-SDK as a direct dependency.
  s.weak_frameworks        = 'GoogleMobileAds'
  s.pod_target_xcconfig    = {
    'FRAMEWORK_SEARCH_PATHS' => '"$(PODS_ROOT)/Google-Mobile-Ads-SDK/Frameworks/**"',
  }

  s.dependency "React"
  s.dependency 'AmazonPublisherServicesSDK', '4.9.2'
  s.dependency 'CriteoPublisherSdk'
  
end

