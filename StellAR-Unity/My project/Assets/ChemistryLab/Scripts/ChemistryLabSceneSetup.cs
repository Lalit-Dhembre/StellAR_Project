using UnityEngine;
using UnityEngine.XR.ARFoundation;

namespace ChemistryLab
{
    using Core;
    using Data;
    using AR;
    using UI;
    using Effects;
    using Interaction;

    /// <summary>
    /// Main scene setup script for the Chemistry Lab.
    /// Attach this to an empty GameObject in your scene to auto-configure everything.
    /// </summary>
    public class ChemistryLabSceneSetup : MonoBehaviour
    {
        [Header("Database")]
        [SerializeField] private ChemicalDatabase chemicalDatabase;

        [Header("Auto-Create Components")]
        [SerializeField] private bool createARManager = true;
        [SerializeField] private bool createUIManager = true;
        [SerializeField] private bool createReactionEngine = true;
        [SerializeField] private bool createReactionEffects = true;

        [Header("References (Auto-populated if null)")]
        [SerializeField] private ChemistryLabARManager arManager;
        [SerializeField] private ChemistryLabUIManager uiManager;
        [SerializeField] private ReactionEngine reactionEngine;
        [SerializeField] private ReactionEffects reactionEffects;

        private void Awake()
        {
            // Hide the template UI first
            if (GetComponent<HideTemplateUI>() == null)
            {
                gameObject.AddComponent<HideTemplateUI>();
            }
            
            SetupScene();
        }

        private void SetupScene()
        {
            // Initialize database
            if (chemicalDatabase != null)
            {
                chemicalDatabase.Initialize();
            }
            else
            {
                Debug.LogWarning("ChemistryLab: No ChemicalDatabase assigned! Run 'Chemistry Lab > Create Default Chemicals' in Unity Editor.");
            }

            // Create ReactionEngine
            if (createReactionEngine && reactionEngine == null)
            {
                var engineObj = new GameObject("ReactionEngine");
                engineObj.transform.SetParent(transform);
                reactionEngine = engineObj.AddComponent<ReactionEngine>();
                
                // Pass the database to the reaction engine
                if (chemicalDatabase != null)
                {
                    reactionEngine.SetDatabase(chemicalDatabase);
                }
                Debug.Log("Created ReactionEngine");
            }

            // Create ReactionEffects
            if (createReactionEffects && reactionEffects == null)
            {
                var effectsObj = new GameObject("ReactionEffects");
                effectsObj.transform.SetParent(transform);
                reactionEffects = effectsObj.AddComponent<ReactionEffects>();
                Debug.Log("Created ReactionEffects");
            }

            // Create Procedural Reaction Effects (visual particles)
            var proceduralEffectsObj = new GameObject("ProceduralReactionEffects");
            proceduralEffectsObj.transform.SetParent(transform);
            proceduralEffectsObj.AddComponent<ProceduralReactionEffects>();
            Debug.Log("Created ProceduralReactionEffects for visual feedback");

            // Create Realistic Effects (enhanced visuals)
            var realisticEffectsObj = new GameObject("RealisticEffects");
            realisticEffectsObj.transform.SetParent(transform);
            realisticEffectsObj.AddComponent<RealisticEffects>();
            Debug.Log("Created RealisticEffects for enhanced visuals");

            // Create AR Manager
            if (createARManager && arManager == null)
            {
                // Check if we have AR Foundation components
                var arSession = FindFirstObjectByType<ARSession>();
                if (arSession != null)
                {
                    var arManagerObj = new GameObject("ChemistryLabARManager");
                    arManagerObj.transform.SetParent(transform);
                    arManager = arManagerObj.AddComponent<ChemistryLabARManager>();
                    Debug.Log("Created ChemistryLabARManager");
                }
                else
                {
                    Debug.LogWarning("ChemistryLab: No ARSession found. AR features will be disabled.");
                }



                // AR Plane Styler removed as per user request (revert to original grid)

            }

            // Create UI Manager  
            if (createUIManager && uiManager == null)
            {
                var uiObj = new GameObject("ChemistryLabUIManager");
                uiObj.transform.SetParent(transform);
                uiManager = uiObj.AddComponent<ChemistryLabUIManager>();
                Debug.Log("Created ChemistryLabUIManager");
            }

            // Create Runtime UI with chemical buttons
            if (createUIManager)
            {
                var runtimeUIObj = new GameObject("ChemistryLabRuntimeUI");
                runtimeUIObj.transform.SetParent(transform);
                var runtimeUI = runtimeUIObj.AddComponent<ChemistryLabRuntimeUI>();
                Debug.Log("Created ChemistryLabRuntimeUI with chemical buttons");
            }

            // Create equipment generator
            var genObj = new GameObject("EquipmentGenerator");
            genObj.transform.SetParent(transform);
            genObj.AddComponent<Equipment.ProceduralEquipmentGenerator>();
            Debug.Log("Created ProceduralEquipmentGenerator");

            // Create 3D model loader (for GLB models)
            var modelLoaderObj = new GameObject("ModelLoader");
            modelLoaderObj.transform.SetParent(transform);
            modelLoaderObj.AddComponent<Equipment.ModelLoader>();
            Debug.Log("Created ModelLoader for 3D GLB models");

            // Create touch interaction handler for dragging equipment
            var touchInteractionObj = new GameObject("LabTouchInteraction");
            touchInteractionObj.transform.SetParent(transform);
            touchInteractionObj.AddComponent<Interaction.LabTouchInteraction>();
            Debug.Log("Created LabTouchInteraction for dragging equipment");



            // Create reaction result UI to show reactions on screen
            var reactionUIObj = new GameObject("ReactionResultUI");
            reactionUIObj.transform.SetParent(transform);
            reactionUIObj.AddComponent<ReactionResultUI>();
            Debug.Log("Created ReactionResultUI for displaying reaction results");

            // Initial equipment is spawned by ChemistryLabARManager when user taps on surface
        }



        /// <summary>
        /// Quick test - spawn a beaker with some chemicals
        /// </summary>
        [ContextMenu("Test Spawn Equipment")]
        public void TestSpawnEquipment()
        {
            if (Equipment.ProceduralEquipmentGenerator.Instance == null)
            {
                var genObj = new GameObject("EquipmentGenerator");
                genObj.AddComponent<Equipment.ProceduralEquipmentGenerator>();
            }

            var beaker = Equipment.ProceduralEquipmentGenerator.Instance.CreateBeaker(null);
            beaker.transform.position = Camera.main.transform.position + Camera.main.transform.forward * 0.5f;
        }

#if UNITY_EDITOR
        [ContextMenu("Setup Default Scene")]
        public void SetupDefaultScene()
        {
            // This is called from Unity Editor
            SetupScene();
            UnityEditor.EditorUtility.SetDirty(gameObject);
        }
#endif
    }
}
