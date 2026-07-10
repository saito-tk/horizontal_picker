# Maven Central 公開手順

このライブラリは、次の Maven 座標で Central Portal へ公開します。

```text
io.github.saito-tk:horizontal-picker:<version>
```

公開には、Verified 状態の `io.github.saito-tk` namespace、Central Portal User Token、keyserver へ登録済みの GPG 公開鍵が必要です。

## 秘密情報

次の値をリポジトリへ保存、またはコミットしてはいけません。

- Central Portal Token の username / password
- GPG 秘密鍵
- GPG パスフレーズ

ローカルではユーザーの `~/.gradle/gradle.properties` または環境変数、CI では Secrets を使います。Gradle が参照する環境変数名は次のとおりです。

```text
ORG_GRADLE_PROJECT_mavenCentralUsername
ORG_GRADLE_PROJECT_mavenCentralPassword
ORG_GRADLE_PROJECT_signingInMemoryKey
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
```

`signingInMemoryKey` には、次のコマンドで取得できる ASCII armored 形式の秘密鍵を渡します。出力をログ、チャット、リポジトリへ貼り付けてはいけません。

```bash
gpg --export-secret-keys --armor <primary-key-fingerprint>
```

## 公開前の確認

公開する version は `picker/build.gradle.kts` の `coordinates` で管理します。一度 Maven Central へ公開した version は上書きできないため、既存 version を再利用してはいけません。

```bash
./gradlew :picker:testDebugUnitTest :picker:assembleRelease :sample:assembleDebug
./gradlew :picker:sourceReleaseJar :picker:javaDocReleaseJar
./gradlew :picker:publishToMavenLocal
```

`publishToMavenLocal` 後は、別の Android プロジェクトから `mavenLocal()` と公開予定の座標だけで依存解決・コンパイルできることを確認します。

## Central Portal へアップロード

初回公開では自動 release を使わず、次のタスクでアップロードだけを行います。

```bash
./gradlew :picker:publishToMavenCentral
```

Central Portal の Deployments 画面で validation 結果、座標、POM、成果物を確認し、問題がなければ手動で `Publish` します。

`publishAndReleaseToMavenCentral` は validation 後に自動で公開するため、初回公開では使用しません。
