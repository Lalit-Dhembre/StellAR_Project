# AR Chemistry Lab - Setup Guide

## Quick Start

### Step 1: Generate Chemical Data
1. Open Unity Editor
2. Go to menu: **Chemistry Lab > Create Default Chemicals**
3. This creates 8 chemicals and 5 reactions in `Assets/ChemistryLab/Data/`

### Step 2: Setup Your Scene
1. Open your AR scene (or create new one with AR template)
2. Create an empty GameObject
3. Add the `ChemistryLabSceneSetup` component
4. Drag the `ChemicalDatabase` asset from `Assets/ChemistryLab/Data/` to the Database field

### Step 3: Build and Run
1. Switch to Android platform: **File > Build Settings > Android > Switch Platform**
2. Ensure ARCore is enabled: **Project Settings > XR Plug-in Management > Android > ARCore**
3. Build and deploy to your Android device

## How to Use

1. **Place the Lab**: Point your phone at a flat surface and tap to place the lab
2. **Select Equipment**: Tap on test tubes or beakers to select them
3. **Add Chemicals**: Open the chemical panel and tap a chemical to add it
4. **Pour**: Tilt containers to pour into others
5. **Heat**: Tap the bunsen burner to toggle, place containers near it to heat
6. **Watch Reactions**: Mix chemicals to see effects!

## MVP Chemicals

| Chemical | Formula | Effect |
|----------|---------|--------|
| Hydrochloric Acid | HCl | Acid (pH 1) |
| Sodium Hydroxide | NaOH | Base (pH 14) |
| Sodium Chloride | NaCl | Salt, yellow flame |
| Silver Nitrate | AgNO₃ | Creates precipitates |
| Copper Sulfate | CuSO₄ | Blue color, green flame |
| Phenolphthalein | C₂₀H₁₄O₄ | Pink in base |
| Sodium | Na | Reacts violently with water! |
| Water | H₂O | Solvent |

## Reactions

1. **HCl + NaOH** → Salt + Water (heat released)
2. **AgNO₃ + NaCl** → White precipitate
3. **CuSO₄ + NaOH** → Blue precipitate
4. **Na + H₂O** → Fizzing + flame!
5. **Phenolphthalein + NaOH** → Pink color

## Folder Structure

```
Assets/ChemistryLab/
├── Scripts/
│   ├── AR/                 - AR integration
│   ├── Containers/         - Test tube, beaker, etc.
│   ├── Core/               - Reaction engine
│   ├── Data/               - Chemical data structures
│   ├── Effects/            - Particle effects
│   ├── Equipment/          - Procedural model generator
│   ├── Interaction/        - Touch/tap handling
│   └── UI/                 - UI management
├── Data/
│   ├── Chemicals/          - Chemical ScriptableObjects
│   └── Reactions/          - Reaction ScriptableObjects
└── ChemistryLab.asmdef
```

## Extending

### Adding New Chemicals
1. Right-click in Project: **Create > Chemistry Lab > Chemical Data**
2. Fill in properties (name, formula, color, pH, etc.)
3. Add to ChemicalDatabase

### Adding New Reactions
1. Right-click: **Create > Chemistry Lab > Reaction**
2. Set reactants and products
3. Choose effect type (precipitate, gas, color change, etc.)
4. Add to ChemicalDatabase

## Troubleshooting

- **Nothing appears?** Make sure you ran "Create Default Chemicals" first
- **Chemicals not reacting?** Check the database has both chemicals and the reaction defined
- **Crashing on device?** Check ARCore is installed on your Android device
