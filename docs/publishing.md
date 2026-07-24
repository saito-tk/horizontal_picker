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

## GitHub Actions で GitHub Release からアップロード

`.github/workflows/publish-maven-central.yml` は、GitHub Release を **Publish** したときに実行されます。単にタグを GitHub へ push しただけでは実行されないため、開発用・検証用のタグとリリースを分けられます。Release に紐づくタグの `v` を除いた version と `picker/build.gradle.kts` の `coordinates` にある version が一致しない場合、アップロード前に失敗します。

リポジトリの **Settings > Secrets and variables > Actions** で、次の Repository secrets を作成してください。値はローカル公開時に使ったものと同じですが、値そのものをリポジトリやログへ保存してはいけません。

| Secret 名 | 設定する値 |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal User Token の username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal User Token の password |
| `SIGNING_IN_MEMORY_KEY` | ASCII armored 形式の GPG 秘密鍵 |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG 秘密鍵のパスフレーズ |

リリース時は、version を更新・検証・コミットした後で、同じ version の注釈付きタグを作成して push します。その後、GitHub でそのタグを指定した Release を作成して **Publish** します。

```bash
git tag -a v<version> -m "Release <version>"
git push origin main
git push origin v<version>
```

例として `coordinates(..., "0.1.2")` に更新した場合は、`v0.1.2` を push して、そのタグを対象に GitHub Release を Publish します。Actions は `:picker:publishToMavenCentral` を実行して Central Portal へアップロードしますが、一般公開までは行いません。Deployments 画面で validation 結果を確認してから、手動で `Publish` を押してください。

workflow 内で使用する GitHub Actions は、可変の version タグではなく commit SHA に固定しています。`.github/dependabot.yml` により、GitHub Actions の更新候補は毎月 Dependabot の PR として通知されます。更新 PR の内容を確認してから merge してください。
