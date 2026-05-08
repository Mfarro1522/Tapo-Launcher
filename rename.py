import os

files_to_update = [
    "app/app/src/main/java/dev/vive/kdelauncher/data/model/AiProvider.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/data/SettingsManagerImpl.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/ui/components/LauncherSettingsPanel.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/ui/components/LabsSection.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/ui/screens/LauncherScreen.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/ui/LauncherViewModel.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/ui/LauncherUiStateMapper.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/domain/repository/SettingsManager.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/domain/usecase/ConnectAiProviderUseCase.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/domain/usecase/OrganizeAppsWithAiUseCase.kt",
    "app/app/src/main/java/dev/vive/kdelauncher/AppContainer.kt"
]

for f in files_to_update:
    if not os.path.exists(f): continue
    with open(f, 'r') as file:
        content = file.read()
    content = content.replace("AiProvider", "AiProviderType")
    # also rename filename if it is AiProvider.kt
    with open(f, 'w') as file:
        file.write(content)

os.rename("app/app/src/main/java/dev/vive/kdelauncher/data/model/AiProvider.kt", "app/app/src/main/java/dev/vive/kdelauncher/data/model/AiProviderType.kt")
