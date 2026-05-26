import Foundation
import GoogleMobileAds
import SwiftUI
import UIKit

private enum StackShiftAdNotification {
    static let showInterstitial = Notification.Name("StackShiftAdShowInterstitial")
    static let interstitialCompleted = Notification.Name("StackShiftAdInterstitialCompleted")
    static let showRewardedRevive = Notification.Name("StackShiftAdShowRewardedRevive")
    static let showRewardedSpecial = Notification.Name("StackShiftAdShowRewardedSpecial")
    static let rewardedCompleted = Notification.Name("StackShiftAdRewardedCompleted")
}

private struct StackShiftAdsProperties {
    private static let scopedPrefix = "ads.ios.stackshift."
    private static let legacyPrefix = "ads.ios."

    static let shared = load()

    let iosApplicationId: String
    let banner: String
    let interstitial: String
    let rewardedRevive: String
    let rewardedSpecial: String

    private static func load() -> StackShiftAdsProperties {
        guard let url = Bundle.main.url(forResource: "ads", withExtension: "properties"),
              let content = try? String(contentsOf: url, encoding: .utf8) else {
            return StackShiftAdsProperties(
                iosApplicationId: "",
                banner: "",
                interstitial: "",
                rewardedRevive: "",
                rewardedSpecial: ""
            )
        }

        let values = parse(content)
        return StackShiftAdsProperties(
            iosApplicationId: value(for: "applicationId", in: values),
            banner: value(for: "bannerUnitId", in: values),
            interstitial: value(for: "interstitialUnitId", in: values),
            rewardedRevive: value(for: "rewardedUnitId", in: values),
            rewardedSpecial: value(for: "rewardedSpecialUnitId", in: values)
        )
    }

    private static func value(for suffix: String, in values: [String: String]) -> String {
        values[scopedPrefix + suffix] ?? values[legacyPrefix + suffix] ?? ""
    }

    private static func parse(_ content: String) -> [String: String] {
        var values: [String: String] = [:]

        for line in content.components(separatedBy: .newlines) {
            let trimmedLine = line.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmedLine.isEmpty, !trimmedLine.hasPrefix("#"), let separatorIndex = trimmedLine.firstIndex(of: "=") else {
                continue
            }

            let key = String(trimmedLine[..<separatorIndex]).trimmingCharacters(in: .whitespaces)
            let value = String(trimmedLine[trimmedLine.index(after: separatorIndex)...]).trimmingCharacters(in: .whitespaces)
            values[key] = value
        }

        return values
    }
}

private enum StackShiftAdMobIDs {
    static let banner = StackShiftAdsProperties.shared.banner
    static let interstitial = StackShiftAdsProperties.shared.interstitial
    static let rewardedRevive = StackShiftAdsProperties.shared.rewardedRevive
    static let rewardedSpecial = StackShiftAdsProperties.shared.rewardedSpecial
}

private enum StackShiftAdMobConfiguration {
    private static let placeholderMarker = "$("

    static let infoPlistApplicationId: String = {
        let value = Bundle.main.object(forInfoDictionaryKey: "GADApplicationIdentifier") as? String
        return normalized(value)
    }()

    static let bundledApplicationId: String = normalized(StackShiftAdsProperties.shared.iosApplicationId)

    static var canInitializeSdk: Bool {
        isValidApplicationId(infoPlistApplicationId)
    }

    static func isValidUnitId(_ value: String) -> Bool {
        let normalizedValue = normalized(value)
        return !normalizedValue.isEmpty &&
            !normalizedValue.contains(placeholderMarker) &&
            normalizedValue.hasPrefix("ca-app-pub-")
    }

    static func logDisabledReasonIfNeeded() {
        guard !canInitializeSdk else { return }

        if isValidApplicationId(bundledApplicationId) {
            print("[Ads] Google Mobile Ads disabled because Info.plist GADApplicationIdentifier is missing or unresolved. Verify ADS_IOS_APPLICATION_ID in GeneratedAds.xcconfig.")
        } else {
            print("[Ads] Google Mobile Ads disabled because no valid iOS AdMob application ID is configured.")
        }
    }

    private static func isValidApplicationId(_ value: String) -> Bool {
        !value.isEmpty &&
            !value.contains(placeholderMarker) &&
            value.hasPrefix("ca-app-pub-")
    }

    private static func normalized(_ value: String?) -> String {
        value?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "\0", with: "") ?? ""
    }
}

final class StackShiftAdBridge: NSObject, FullScreenContentDelegate {
    static let shared = StackShiftAdBridge()

    private var started = false
    private var observerTokens: [NSObjectProtocol] = []
    private var interstitialAd: InterstitialAd?
    private var rewardedReviveAd: RewardedAd?
    private var rewardedSpecialAd: RewardedAd?
    private var pendingInterstitialRequestId: String?
    private var pendingRewardedRequestId: String?
    private var didEarnReward = false

    private override init() {
        super.init()
    }

    func start() {
        guard !started else { return }
        started = true

        registerObservers()

        guard StackShiftAdMobConfiguration.canInitializeSdk else {
            StackShiftAdMobConfiguration.logDisabledReasonIfNeeded()
            return
        }

        MobileAds.shared.start(completionHandler: nil)
        loadInterstitial()
        loadRewardedRevive()
        loadRewardedSpecial()
    }

    deinit {
        observerTokens.forEach(NotificationCenter.default.removeObserver)
    }

    private func registerObservers() {
        let center = NotificationCenter.default

        observerTokens.append(
            center.addObserver(
                forName: StackShiftAdNotification.showInterstitial,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                self?.handleInterstitialRequest(notification)
            }
        )

        observerTokens.append(
            center.addObserver(
                forName: StackShiftAdNotification.showRewardedRevive,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                self?.handleRewardedReviveRequest(notification)
            }
        )

        observerTokens.append(
            center.addObserver(
                forName: StackShiftAdNotification.showRewardedSpecial,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                self?.handleRewardedSpecialRequest(notification)
            }
        )
    }

    private func handleInterstitialRequest(_ notification: Notification) {
        guard let requestId = notification.object as? String, !requestId.isEmpty else { return }
        guard let ad = interstitialAd, let rootViewController = Self.topViewController() else {
            postInterstitialCompleted(requestId: requestId)
            loadInterstitial()
            return
        }

        interstitialAd = nil
        pendingInterstitialRequestId = requestId
        ad.fullScreenContentDelegate = self
        ad.present(from: rootViewController)
    }

    private func handleRewardedReviveRequest(_ notification: Notification) {
        guard let requestId = notification.object as? String, !requestId.isEmpty else { return }
        guard let ad = rewardedReviveAd, let rootViewController = Self.topViewController() else {
            postRewardedCompleted(requestId: requestId, rewarded: false)
            loadRewardedRevive()
            return
        }

        rewardedReviveAd = nil
        pendingRewardedRequestId = requestId
        didEarnReward = false
        ad.fullScreenContentDelegate = self
        ad.present(from: rootViewController) { [weak self] in
            self?.didEarnReward = true
        }
    }

    private func handleRewardedSpecialRequest(_ notification: Notification) {
        guard let requestId = notification.object as? String, !requestId.isEmpty else { return }
        guard let ad = rewardedSpecialAd, let rootViewController = Self.topViewController() else {
            postRewardedCompleted(requestId: requestId, rewarded: false)
            loadRewardedSpecial()
            return
        }

        rewardedSpecialAd = nil
        pendingRewardedRequestId = requestId
        didEarnReward = false
        ad.fullScreenContentDelegate = self
        ad.present(from: rootViewController) { [weak self] in
            self?.didEarnReward = true
        }
    }

    private func loadInterstitial() {
        guard StackShiftAdMobConfiguration.canInitializeSdk,
              StackShiftAdMobConfiguration.isValidUnitId(StackShiftAdMobIDs.interstitial) else { return }
        InterstitialAd.load(
            with: StackShiftAdMobIDs.interstitial,
            request: Request()
        ) { [weak self] ad, _ in
            self?.interstitialAd = ad
        }
    }

    private func loadRewardedRevive() {
        guard StackShiftAdMobConfiguration.canInitializeSdk,
              StackShiftAdMobConfiguration.isValidUnitId(StackShiftAdMobIDs.rewardedRevive) else { return }
        RewardedAd.load(
            with: StackShiftAdMobIDs.rewardedRevive,
            request: Request()
        ) { [weak self] ad, _ in
            self?.rewardedReviveAd = ad
        }
    }

    private func loadRewardedSpecial() {
        guard StackShiftAdMobConfiguration.canInitializeSdk,
              StackShiftAdMobConfiguration.isValidUnitId(StackShiftAdMobIDs.rewardedSpecial) else { return }
        RewardedAd.load(
            with: StackShiftAdMobIDs.rewardedSpecial,
            request: Request()
        ) { [weak self] ad, _ in
            self?.rewardedSpecialAd = ad
        }
    }

    private func finishInterstitialIfNeeded() {
        guard let requestId = pendingInterstitialRequestId else { return }
        pendingInterstitialRequestId = nil
        postInterstitialCompleted(requestId: requestId)
        loadInterstitial()
    }

    private func finishRewardedIfNeeded(rewarded: Bool) {
        guard let requestId = pendingRewardedRequestId else { return }
        pendingRewardedRequestId = nil
        didEarnReward = false
        postRewardedCompleted(requestId: requestId, rewarded: rewarded)
        loadRewardedRevive()
        loadRewardedSpecial()
    }

    private func postInterstitialCompleted(requestId: String) {
        NotificationCenter.default.post(name: StackShiftAdNotification.interstitialCompleted, object: requestId)
    }

    private func postRewardedCompleted(requestId: String, rewarded: Bool) {
        NotificationCenter.default.post(name: StackShiftAdNotification.rewardedCompleted, object: "\(requestId):\(rewarded)")
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        if pendingInterstitialRequestId != nil {
            finishInterstitialIfNeeded()
        } else {
            finishRewardedIfNeeded(rewarded: didEarnReward)
        }
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        if pendingInterstitialRequestId != nil {
            finishInterstitialIfNeeded()
        } else {
            finishRewardedIfNeeded(rewarded: false)
        }
    }

    static func topViewController(
        base: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    ) -> UIViewController? {
        if let navigationController = base as? UINavigationController {
            return topViewController(base: navigationController.visibleViewController)
        }
        if let tabBarController = base as? UITabBarController {
            return topViewController(base: tabBarController.selectedViewController)
        }
        if let presentedViewController = base?.presentedViewController {
            return topViewController(base: presentedViewController)
        }
        return base
    }
}

struct StackShiftBannerHost: UIViewRepresentable {
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = .clear

        let placeholderView = makePlaceholderView()
        placeholderView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(placeholderView)
        NSLayoutConstraint.activate([
            placeholderView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            placeholderView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            placeholderView.topAnchor.constraint(equalTo: container.topAnchor),
            placeholderView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])

        guard StackShiftAdMobConfiguration.canInitializeSdk,
              StackShiftAdMobConfiguration.isValidUnitId(StackShiftAdMobIDs.banner) else {
            context.coordinator.placeholderView = placeholderView
            return container
        }

        let bannerView = BannerView()
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        bannerView.adUnitID = StackShiftAdMobIDs.banner
        bannerView.rootViewController = StackShiftAdBridge.topViewController()
        bannerView.delegate = context.coordinator
        bannerView.isHidden = true

        container.addSubview(bannerView)
        NSLayoutConstraint.activate([
            bannerView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            bannerView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            bannerView.topAnchor.constraint(equalTo: container.topAnchor),
            bannerView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])

        context.coordinator.bannerView = bannerView
        context.coordinator.placeholderView = placeholderView
        return container
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        guard StackShiftAdMobConfiguration.canInitializeSdk,
              StackShiftAdMobConfiguration.isValidUnitId(StackShiftAdMobIDs.banner) else { return }
        guard let bannerView = context.coordinator.bannerView else { return }

        bannerView.rootViewController = StackShiftAdBridge.topViewController()
        let width = max(floor(uiView.bounds.width), floor(UIScreen.main.bounds.width))
        guard width > 0 else { return }
        guard context.coordinator.loadedWidth != width else { return }

        context.coordinator.loadedWidth = width
        bannerView.adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
        bannerView.load(Request())
    }

    private func makePlaceholderView() -> UIView {
        let view = UIView()
        view.backgroundColor = UIColor.secondarySystemBackground.withAlphaComponent(0.96)

        let accentBar = UIView()
        accentBar.translatesAutoresizingMaskIntoConstraints = false
        accentBar.backgroundColor = UIColor.systemTeal.withAlphaComponent(0.88)
        accentBar.layer.cornerRadius = 6

        let titleLabel = UILabel()
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.text = "StackShift"
        titleLabel.font = UIFont.systemFont(ofSize: 14, weight: .bold)
        titleLabel.textColor = UIColor.label

        let badgeLabel = UILabel()
        badgeLabel.translatesAutoresizingMaskIntoConstraints = false
        badgeLabel.text = "ad"
        badgeLabel.font = UIFont.systemFont(ofSize: 11, weight: .semibold)
        badgeLabel.textColor = UIColor.secondaryLabel
        badgeLabel.backgroundColor = UIColor.white.withAlphaComponent(0.08)
        badgeLabel.layer.cornerRadius = 8
        badgeLabel.layer.masksToBounds = true
        badgeLabel.textAlignment = .center

        view.addSubview(accentBar)
        view.addSubview(titleLabel)
        view.addSubview(badgeLabel)

        NSLayoutConstraint.activate([
            accentBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 12),
            accentBar.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            accentBar.widthAnchor.constraint(equalToConstant: 18),
            accentBar.heightAnchor.constraint(equalToConstant: 18),

            titleLabel.leadingAnchor.constraint(equalTo: accentBar.trailingAnchor, constant: 10),
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),

            badgeLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -12),
            badgeLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            badgeLabel.widthAnchor.constraint(equalToConstant: 30),
            badgeLabel.heightAnchor.constraint(equalToConstant: 18),

            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: badgeLabel.leadingAnchor, constant: -8)
        ])

        return view
    }

    final class Coordinator: NSObject, BannerViewDelegate {
        var loadedWidth: CGFloat = 0
        weak var bannerView: BannerView?
        weak var placeholderView: UIView?

        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            bannerView.isHidden = false
            placeholderView?.isHidden = true
        }

        func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: any Error) {
            bannerView.isHidden = true
            placeholderView?.isHidden = false
        }
    }
}

