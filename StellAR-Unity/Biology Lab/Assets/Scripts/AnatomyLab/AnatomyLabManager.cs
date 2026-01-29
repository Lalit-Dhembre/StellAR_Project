using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace AnatomyLab
{
    public class AnatomyLabManager : MonoBehaviour
    {
        public static AnatomyLabManager Instance { get; private set; }

        [SerializeField]
        private AnatomyUIManager uiManager;

        private void Awake()
        {
            if (Instance == null)
            {
                Instance = this;
            }
            else
            {
                Destroy(gameObject);
                return;
            }

            if (uiManager == null)
            {
                uiManager = FindObjectOfType<AnatomyUIManager>();
                if (uiManager == null)
                {
                    CreateDefaultUI();
                }
            }
        }

        private void CreateDefaultUI()
        {
            Debug.Log("AnatomyLabManager: Creating default UI.");

            // Create Canvas
            GameObject canvasObj = new GameObject("AnatomyCanvas");
            Canvas canvas = canvasObj.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvasObj.AddComponent<CanvasScaler>();
            canvasObj.AddComponent<GraphicRaycaster>();

            // Create Panel
            GameObject panelObj = new GameObject("InfoPanel");
            panelObj.transform.SetParent(canvasObj.transform, false);
            Image panelImage = panelObj.AddComponent<Image>();
            panelImage.color = new Color(0, 0, 0, 0.8f);
            RectTransform panelRect = panelObj.GetComponent<RectTransform>();
            panelRect.anchorMin = new Vector2(0.5f, 0); // Bottom center
            panelRect.anchorMax = new Vector2(0.5f, 0);
            panelRect.pivot = new Vector2(0.5f, 0);
            panelRect.anchoredPosition = new Vector2(0, 50);
            panelRect.sizeDelta = new Vector2(400, 200);

            // Create Title Text
            GameObject titleObj = new GameObject("TitleText");
            titleObj.transform.SetParent(panelObj.transform, false);
            TextMeshProUGUI titleText = titleObj.AddComponent<TextMeshProUGUI>();
            titleText.alignment = TextAlignmentOptions.Center;
            titleText.fontSize = 24;
            titleText.fontStyle = FontStyles.Bold;
            RectTransform titleRect = titleObj.GetComponent<RectTransform>();
            titleRect.anchorMin = new Vector2(0, 1);
            titleRect.anchorMax = new Vector2(1, 1);
            titleRect.pivot = new Vector2(0.5f, 1);
            titleRect.anchoredPosition = new Vector2(0, -10);
            titleRect.sizeDelta = new Vector2(0, 40);

            // Create Description Text
            GameObject descObj = new GameObject("DescriptionText");
            descObj.transform.SetParent(panelObj.transform, false);
            TextMeshProUGUI descText = descObj.AddComponent<TextMeshProUGUI>();
            descText.alignment = TextAlignmentOptions.TopLeft;
            descText.fontSize = 16;
            RectTransform descRect = descObj.GetComponent<RectTransform>();
            descRect.anchorMin = new Vector2(0, 0);
            descRect.anchorMax = new Vector2(1, 1);
            descRect.pivot = new Vector2(0.5f, 0.5f);
            descRect.offsetMin = new Vector2(10, 10);
            descRect.offsetMax = new Vector2(-10, -50); // Below title

            // Setup UI Manager
            uiManager = canvasObj.AddComponent<AnatomyUIManager>();
            
            // We need to use reflection or modify AnatomyUIManager to allow setting private fields, 
            // OR just rely on AnatomyUIManager.Awake/Start if it finds them, 
            // BUT simpler is to just assign them if we make them public or have a setter.
            // For now, let's assume we can set them via SerializedObject or just GetComponent in AnatomyUIManager if we modify it too.
            // Actually, let's modify AnatomyUIManager to have a public Setup method.
            
            uiManager.SetupReferences(panelObj, titleText, descText);
        }

        public void ShowInfo(string name, string description)
        {
            if (uiManager != null)
            {
                uiManager.UpdateInfoPanel(name, description);
            }
            else
            {
                Debug.LogWarning("AnatomyUIManager is not assigned in AnatomyLabManager.");
            }
        }

        public void HideInfo()
        {
            if (uiManager != null)
            {
                uiManager.CloseInfoPanel();
            }
        }
    }
}
