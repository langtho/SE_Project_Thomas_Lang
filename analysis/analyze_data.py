import pandas as pd
import json
import matplotlib.pyplot as plt
import numpy as np
from scipy.interpolate import make_interp_spline
import seaborn as sns 
import os
import io

def plot_function_time_comparison(df, function_type):
    """
    Vergleicht die Laufzeit (Y) aller Kompressionstypen für einen bestimmten function_type.
    Jeder Kompressionstyp ist eine Linie, eingefärbt nach 'valueSize'.
    """
    
    # 1. Daten filtern und bereinigen (nur der ausgewählte Funktionstyp und positive Zeiten)
    filtered_df = df[(df['functionType'] == function_type) & (df['fullDurationNanos'] > 0)].copy()
    if filtered_df.empty:
        print(f"Keine gültigen Daten für den Vergleich des Funktionstyps '{function_type}'.")
        return
        
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    
    # 2. Aggregieren: Durchschnittliche Zeit pro Array-Größe, Kompressionstyp und Wertgröße
    avg_df = filtered_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['fullTimeMillis'].mean().reset_index()

    plt.figure(figsize=(14, 9))
    
    # 3. Definiere Farbschemata (Value Size) und Linien-Styles (Compression Type)
    value_colors = {'small_v': 'blue', 'medium_v': 'green', 'large_v': 'red'}
    comp_styles = {'NonSpanning': 'solid', 'Spanning': 'dashed', 'Overflow': 'dotted'}
    
    # 4. Iteriere und Plotte Linien
    for comp_type, style in comp_styles.items():
        for value_size, color in value_colors.items():
            subset = avg_df[
                (avg_df['compressionType'] == comp_type) & 
                (avg_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) >= 2:
                # Plotten der ungesmoothten Durchschnittslinie
                plt.plot(
                    subset['uncompressedArraySize'], 
                    subset['fullTimeMillis'], 
                    color=color, 
                    linestyle=style,
                    linewidth=2,
                    alpha=0.8,
                    label=f'{comp_type} ({value_size})'
                )

    # 5. Achsen-Konfiguration
    # plt.xscale('log') <-- ENTFERNT
    plt.ylim(bottom=0)
    
    # Y-Achsen-Begrenzung auf das 95. Perzentil
    positive_times = filtered_df['fullTimeMillis']
    max_y_limit = np.percentile(positive_times, 95) * 1.05 if not positive_times.empty else None
    if max_y_limit is not None:
         plt.ylim(top=max_y_limit)

    plt.xlabel('Uncompressed Array Size', fontsize=12) # Angepasst
    plt.ylabel('Average Time (ms)', fontsize=12)
    plt.title(f'Comparison of Compression Types for: {function_type}', fontsize=16)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Compression / Value Size', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()

def plot_function_size_comparison(df):
    """
    Vergleicht die komprimierte Größe aller Kompressionstypen vs. Array-Größe (nur Compress).
    """
    
    # Filtere nur Compress-Daten
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty:
        print("Keine Compress-Daten gefunden für den Größenvergleich.")
        return
        
    # Aggregieren: Durchschnittliche Größe pro Array-Größe, Kompressionstyp und Wertgröße
    avg_df = compress_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['compressedArraySize'].mean().reset_index()

    plt.figure(figsize=(14, 9))
    max_size = compress_df['uncompressedArraySize'].max()
    
    # 1. Null-Kompression (als Referenz)
    plt.plot([0, max_size], [0, max_size], color='black', linestyle='--', label='Zero compression line (Baseline)')
    
    # 2. Definiere Farbschemata (Value Size) und Linien-Styles (Compression Type)
    value_colors = {'small_v': 'blue', 'medium_v': 'green', 'large_v': 'red'}
    comp_styles = {'NonSpanning': 'solid', 'Spanning': 'dashed', 'Overflow': 'dotted'}

    # 3. Iteriere und Plotte Linien
    for comp_type, style in comp_styles.items():
        for value_size, color in value_colors.items():
            subset = avg_df[
                (avg_df['compressionType'] == comp_type) & 
                (avg_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) >= 2:
                # Plotten der ungesmoothten Durchschnittslinie
                plt.plot(
                    subset['uncompressedArraySize'], 
                    subset['compressedArraySize'], 
                    color=color, 
                    linestyle=style,
                    linewidth=2,
                    alpha=0.8,
                    label=f'{comp_type} ({value_size})'
                )

    # 4. Achsen-Konfiguration
    # plt.xscale('log') <-- ENTFERNT
    # plt.yscale('log') # <-- ENTFERNT
    
    plt.xlabel('Uncompressed Array Size', fontsize=12) # Angepasst
    plt.ylabel('Average Compressed Size', fontsize=12) # Angepasst
    plt.title('Comparison of Compression Types: Compressed Size vs. Array Size', fontsize=16)
    plt.grid(True, which="major", linestyle='--', alpha=0.6)
    plt.legend(title='Compression / Value Size', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()

def plot_compression_ratio(df):
    """
    Plots the average compression ratio vs. uncompressed array size.
    Zeigt geglättete Linien für jeden Kompressionstyp, eingefärbt nach 'valueSize'.
    """
    # Filtere nur Compress-Daten, da das Verhältnis nur hier Sinn ergibt
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty:
        print("Keine Compress-Daten gefunden für das Kompressionsraten-Diagramm.")
        return

    # Berechne das Kompressionsverhältnis (Ratio: 1 = perfekt, 0 = keine Reduktion)
    # Ratio = 1 - (Compressed Size / Uncompressed Size)
    compress_df['ratio'] = 1 - (compress_df['compressedArraySize'] / compress_df['uncompressedArraySize'])
    
    # Gruppiere für den Durchschnitt (pro Kompressionstyp und Wertgröße)
    avg_ratio_df = compress_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['ratio'].mean().reset_index()

    plt.figure(figsize=(12, 8))
    
    # 1. Definiere Linien-Styles für die Kompressionstypen
    line_styles = {'NonSpanning': 'solid', 'Spanning': 'solid', 'Overflow': 'solid'}
    color_map = {'small_v': 'blue', 'medium_v': 'orange', 'large_v': 'red', 'mixed_v': 'yellow'}
    
    # 2. Plotte die Linien
    for comp_type in avg_ratio_df['compressionType'].unique():
        for value_size, color in color_map.items():
            subset = avg_ratio_df[
                (avg_ratio_df['compressionType'] == comp_type) & 
                (avg_ratio_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) > 2:
                # Glättungslogik (wie zuvor)
                X_data = subset['uncompressedArraySize'].values
                Y_data = subset['ratio'].values
                
                # Entfernung der Log-Skala für die Glättung (macht jetzt keine Log-Glättung)
                spl = make_interp_spline(X_data, Y_data, k=3)
                X_smooth = np.linspace(X_data.min(), X_data.max(), 500)
                Y_smooth = spl(X_smooth)
                
                plt.plot(
                    X_smooth, 
                    Y_smooth, 
                    color=color, 
                    linestyle=line_styles.get(comp_type, '-'),
                    label=f'{comp_type} ({value_size})'
                )

    # plt.xscale('log') <-- ENTFERNT
    plt.ylim(0, 1.05) # Rate liegt zwischen 0 und 1 (1 = 100% Kompression)
    plt.xlabel('Uncompressed Array Size', fontsize=12) # Angepasst
    plt.ylabel('Average Compression Ratio (0-1)', fontsize=12)
    plt.title('Compression Ratio vs. Array Size (Effectiveness)', fontsize=14)
    plt.grid(True, which="both", linestyle='--', alpha=0.5)
    plt.legend(title='Kombination', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()

def plot_efficiency(df):
    """
    Plots Compression Ratio (Y) vs. Total Time (X). 
    Punkte sind nach 'compressionType' eingefärbt und nach 'arraySize' skaliert.
    """
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty: return

    # Berechne das Kompressionsverhältnis
    compress_df['ratio'] = 1 - (compress_df['compressedArraySize'] / compress_df['uncompressedArraySize'])
    
    # Definiere die Farben für die Kompressionstypen
    color_map = {'NonSpanning': 'blue', 'Spanning': 'green', 'Overflow': 'red'}
    
    plt.figure(figsize=(12, 8))
    
    # Skaliere die Größe der Punkte basierend auf der Array-Größe 
    # (Wir verwenden die unkomprimierte Größe als Indikator)
    min_size = compress_df['uncompressedArraySize'].min()
    max_size = compress_df['uncompressedArraySize'].max()
    # Skaliere die Größe so, dass sie sichtbar ist, z.B. von 20 bis 200
    compress_df['size_scaled'] = 20 + 180 * ((compress_df['uncompressedArraySize'] - min_size) / (max_size - min_size))

    for comp_type, color in color_map.items():
        subset = compress_df[compress_df['compressionType'] == comp_type]
        if not subset.empty:
            plt.scatter(
                subset['fullTimeMillis'], 
                subset['ratio'], 
                s=subset['size_scaled'], # Größe der Punkte
                color=color, 
                alpha=0.6,
                label=comp_type
            )
            
    plt.xlim(left=0) # Zeit beginnt bei 0
    plt.ylim(0, 1.05) # Ratio liegt zwischen 0 und 1
    # plt.xscale('log') <-- ENTFERNT
    
    plt.xlabel('Total Time (ms) (Lower is Better)', fontsize=12)
    plt.ylabel('Compression Ratio (0-1) (Higher is Better)', fontsize=12)
    plt.title('Efficiency: Compression Ratio vs. Time', fontsize=14)
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.legend(title='Compression Type', loc='lower right')
    
    # Füge eine Legende für die Punktgröße hinzu (Array Size)
    for size, label in zip([min_size, (min_size + max_size) / 2, max_size], ['Small', 'Medium', 'Large']):
        scaled_size = 20 + 180 * ((size - min_size) / (max_size - min_size))
        plt.scatter([], [], c='gray', alpha=0.6, s=scaled_size, label=f'Array Size: {label}')
    plt.legend(scatterpoints=1, frameon=True, labelspacing=1, title='Array Size', loc='upper right')
    
    plt.tight_layout()


def plot_category_heatmap(df, compression_type, function_type):
    """
    Plots a heatmap of average time grouped by arraySize and valueSize.
    """
    # Filtere für den ausgewählten Typ
    filtered_df = df[
        (df['compressionType'] == compression_type) & 
        (df['functionType'] == function_type)
    ].copy()

    if filtered_df.empty: return

    # Berechne die durchschnittliche Zeit für jede Kategoriekombination
    pivot_df = filtered_df.groupby(['arraySize', 'valueSize'])['fullTimeMillis'].mean().unstack()

    if pivot_df.empty:
        print(f"Keine ausreichenden Daten für Heatmap für {compression_type} ({function_type}).")
        return

    # Definiere die Reihenfolge der Achsen für bessere Lesbarkeit
    array_order = ['small_s', 'small_medium_s', 'medium_s', 'medium_large_s', 'large_s']
    value_order = ['small_v', 'small_medium_v', 'medium_v', 'medium_large_v', 'large_v', 'mixed_v']
    
    pivot_df = pivot_df.reindex(index=array_order, columns=value_order).dropna(axis=0, how='all').dropna(axis=1, how='all')

    plt.figure(figsize=(10, 7))
    # Verwende Seaborn für eine ästhetische Heatmap
    sns.heatmap(
        pivot_df, 
        annot=True,     # Zeige die Werte im Plot an
        fmt=".2f",      # Formatiere auf 2 Dezimalstellen
        cmap="YlOrRd",  # Color Map von Gelb über Orange zu Rot (Rot = langsam)
        linewidths=.5,  # Linien zwischen den Zellen
        cbar_kws={'label': 'Average Time (ms)'}
    )
    
    plt.title(f'Average Time Heatmap for {compression_type} ({function_type})', fontsize=14)
    plt.xlabel('Value Size Category', fontsize=12)
    plt.ylabel('Array Size Category', fontsize=12)
    plt.xticks(rotation=45, ha='right')
    plt.yticks(rotation=0)
    plt.tight_layout()

def plot_time_vs_array_size(df, compression_type, function_type):
    """
    Plots a scatter plot of total time vs. uncompressed array size.
    Verwendet eine lineare Y-Achse und begrenzt die Y-Achse 
    dynamisch auf das 95. Perzentil der Zeitdaten.
    """
    
    # 1. Definiere die Farben für jede 'valueSize' (Color Map)
    color_map = {
        'small_v': 'blue',
        'small_medium_v': 'green',
        'medium_v': 'orange',
        'medium_large_v': 'red',
        'large_v': 'purple',
        'mixed_v': 'yellow'
    }
    
    plt.figure(figsize=(12, 8))
    
    # 2. Iteriere über jede 'valueSize' für das Scatter-Plot
    for value_size, color in color_map.items():
        subset = df[df['valueSize'] == value_size]
        
        if not subset.empty:
            plt.scatter(
                subset['uncompressedArraySize'], 
                subset['fullTimeMillis'], 
                color=color, 
                alpha=0.6,
                label=f'Value Size: {value_size}'
            )
            
    # *** Dynamische Y-Achsen-Begrenzung ***
    
    # Nur gültige, positive Zeiten betrachten
    positive_times = df['fullTimeMillis'][df['fullTimeMillis'] > 0]
    
    if not positive_times.empty:
        # Berechne das 95. Perzentil der positiven Zeiten
        percentile_95 = np.percentile(positive_times, 95)
        
        # Lege die Obergrenze fest, mit einem kleinen Puffer (z.B. 5%)
        max_y_limit = percentile_95 * 1.05
        
        # Wende die Begrenzung an
        plt.ylim(bottom=0, top=max_y_limit)
        
        print(f"  Y-Achse begrenzt auf: {max_y_limit:.2f} ms (95. Perzentil: {percentile_95:.2f} ms)")
    else:
        # Sicherstellen, dass die Achse bei 0 beginnt, falls keine Daten da sind
        plt.ylim(bottom=0)
            
    # 3. Wende die Skalierung an
    # plt.xscale('log') <-- ENTFERNT
            
    plt.xlabel('Uncompressed Array Size', fontsize=12) # Angepasst
    plt.ylabel('Time (ms)', fontsize=12)
    plt.title(f'Time vs. Array-Size for {compression_type} ({function_type}) (Limited Linear Time Scale)', fontsize=14)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Value Size Category', loc='upper left')
    plt.tight_layout()

def plot_compressed_size_vs_array_size(df, compression_type):
    """
    Plots a scatter plot of compressed size vs. uncompressed array size with a zero-compression line.
    Plottet einen Scatter-Plot der komprimierten Größe gegen die unkomprimierte Array-Größe
    zusammen mit einer "Null-Kompression"-Linie.
    """

    color_map = {
        'small_v': 'blue',
        'small_medium_v': 'green',
        'medium_v': 'orange',
        'medium_large_v': 'red',
        'large_v': 'purple',
        'mixed_v': 'yellow'
    }
    df['color']=df['valueSize'].map(color_map).fillna('grey')

    plt.figure(figsize=(10, 6))
    max_size = df['uncompressedArraySize'].max()
    # Null-Kompression: Komprimierte Größe = Unkomprimierte Größe
    plt.plot([0, max_size], [0, max_size], color='red', linestyle='--', label='Zero compression line')
    for value_size, color in color_map.items():
            subset = df[df['valueSize'] == value_size]
            if not subset.empty:
                plt.scatter(
                    subset['uncompressedArraySize'], 
                    subset['compressedArraySize'], 
                    color=color, 
                    alpha=0.7,
                    label=f'Value Size: {value_size}',
                    zorder=2 # Punkte über der roten Linie zeichnen
                )
    plt.xlabel('Array-Size', fontsize=12)
    plt.ylabel('Compressed Size', fontsize=12)
    plt.title(f'Compressed Size vs. Uncompressed Array-Size for {compression_type}', fontsize=14)
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend()
    plt.tight_layout()

def plot_stacked_bar_chart(df, compression_type, function_type):
    """
    Plots a stacked bar chart of average part times grouped by the categorical array size ('arraySize').
    Fügt die gesamte durchschnittliche Zeit über jedem Balken als Text-Annotation hinzu.
    """
    parts_data_list = []
    
    # 1. Sammle alle 'parts'-Daten und behalte die 'arraySize'-Kategorie bei
    for _, row in df.iterrows():
        # Stelle sicher, dass 'parts' vorhanden ist und eine Liste ist
        if isinstance(row.get('parts'), list):
            for part in row['parts']:
                parts_data_list.append({
                    'arraySize': row['arraySize'],
                    'partName': part['name'],
                    'timeNanos': part['timeNanos']
                })
    
    if not parts_data_list:
        print(f"  Achtung: Keine 'part'-Daten gefunden für {compression_type} ({function_type}). Das Diagramm wird übersprungen.")
        return
        
    parts_df = pd.DataFrame(parts_data_list)
    
    # 2. Definiere die Reihenfolge der X-Achse
    size_order = ['small_s', 'small_medium_s', 'medium_s', 'medium_large_s', 'large_s']
    parts_df = parts_df[parts_df['arraySize'].isin(size_order)]
    
    # Berechne die durchschnittlichen Zeiten pro KATEGORISCHER Array-Größe und Teil
    avg_times = parts_df.groupby(['arraySize', 'partName'])['timeNanos'].mean().reset_index()
    avg_times['timeMillis'] = avg_times['timeNanos'] / 1_000_000
    
    # 3. Pivot-Tabelle erstellen
    pivot_df = avg_times.pivot_table(
        index='arraySize', 
        columns='partName', 
        values='timeMillis', 
        aggfunc='sum'
    ).fillna(0)
    
    # 4. Wende die definierte Reihenfolge auf den Index an
    plot_order = [size for size in size_order if size in pivot_df.index]
    pivot_df = pivot_df.reindex(plot_order)
    
    # *** NEU: Gesamtdurchschnittszeit berechnen ***
    # Summiere die Zeiten über alle 'Parts' für jede 'arraySize'
    total_avg_time = pivot_df.sum(axis=1)
    
    # 5. Plotten
    ax = pivot_df.plot(kind='bar', stacked=True, figsize=(12, 8))
    
    # *** NEU: Text-Annotationen hinzufügen ***
    # Iteriere über die Indizes der Balken (0, 1, 2, ...) und die Gesamtzeiten
    for i, total in enumerate(total_avg_time.values):
        # x-Koordinate: Index des Balkens (i)
        # y-Koordinate: Die Gesamthöhe des Balkens (total) + ein kleiner Abstand (z.B. 0.5)
        # Text: Die Gesamtzeit, auf 2 Dezimalstellen formatiert
        ax.text(
            x=i, 
            y=total + total_avg_time.max() * 0.01, # Etwas Abstand vom Balken
            s=f'{total:.2f} ms', # Gesamtzeit (z.B. "12.34 ms")
            ha='center', # Horizontal zentriert über dem Balken
            va='bottom', # Vertikal knapp über dem Balken
            fontsize=10,
            fontweight='bold'
        )

    plt.title(f'Average time for {compression_type} ({function_type}) (Total Time Annotated)', fontsize=16)
    plt.xlabel('Array Size Category', fontsize=12)
    plt.ylabel('Average time (ms)', fontsize=12)
    plt.xticks(rotation=0) 
    plt.legend(title='Parts', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    # Stellt sicher, dass der Platz für die Text-Annotationen ausreicht
    plt.subplots_adjust(top=0.9)

# ----------------------------------------------------------------------
# NEUE MODULARE FUNKTION
# ----------------------------------------------------------------------

def process_and_plot(df, compression_type, function_type):
    """
    Filtert den DataFrame, berechnet die Zeit in Millisekunden und erstellt 
    alle relevanten Diagramme für die angegebene Kompressions- und Funktionstyp-Kombination.
    """
    
    # Filtern des DataFrames
    filtered_df = df[
        (df['compressionType'] == compression_type) & 
        (df['functionType'] == function_type)
    ].copy()

    if filtered_df.empty:
        print(f"Keine Daten gefunden für compressionType '{compression_type}' und functionType '{function_type}'.")
        return
    
    print(f"\n--- Erstelle Diagramme für: {compression_type} - {function_type} ---")
    
    # Berechnung der Gesamtzeit in Millisekunden
    # Stelle sicher, dass diese Spalte existiert, bevor sie verwendet wird
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000

    # 1. Time vs. Array-Size (Immer relevant)
    plot_time_vs_array_size(filtered_df, compression_type, function_type)
    
    # 2. Compressed Size vs. Array-Size (Nur für 'Compress' relevant)
    if function_type == 'Compress':
        plot_compressed_size_vs_array_size(filtered_df, compression_type)
    
    # 3. Stacked Bar Chart (Immer relevant)
    plot_stacked_bar_chart(filtered_df, compression_type, function_type)

    plot_category_heatmap(filtered_df,compression_type,function_type)

    plot_efficiency(filtered_df)

    plot_compression_ratio(filtered_df)


def main():
    # Pfad zur jsonl-Datei
    file_path = '../src/main/resources/performance_data.jsonl'
    df = None # Initialisiere df außerhalb des try-Blocks

    # 1. Daten laden
    try:
        print(f"Lese Daten von: {file_path}")
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = [line.strip() for line in f if line.strip()]
        parsed_data = [json.loads(line) for line in lines]
        df = pd.DataFrame(parsed_data)
        print(f"Daten erfolgreich geladen. {len(df)} Zeilen gefunden.")
    except FileNotFoundError:
        print(f"Fehler: Die Datei '{file_path}' existiert nicht.")
        return
    except json.JSONDecodeError as e:
        print(f"Fehler beim Parsen von JSON in Zeile: {e.lineno}. Fehler: {e.msg}")
        return
        
    # Wenn df nicht geladen wurde (z.B. bei leerer Datei)
    if df is None or df.empty:
        print("Der geladene DataFrame ist leer. Es können keine Diagramme erstellt werden.")
        return

    # 2. Definition aller zu plottenden Kombinationen
    # Diese Liste kannst du einfach erweitern!
    plot_combinations = [
        # Compress-Operationen
        #('NonSpanning', 'Compress'),
        #('Spanning', 'Compress'),
        #('Overflow', 'Compress'),
        
        # Decompress-Operationen (falls Daten vorhanden)
        #('NonSpanning', 'Decompress'),
        #('Spanning', 'Decompress'),
        ('Overflow', 'Decompress'),

        #('NonSpanning', 'get'),
        #('Spanning', 'get'),
        ('Overflow', 'get'),
    ]

    # 3. Iteration über alle Kombinationen und Aufruf der neuen Funktion
    for comp_type, func_type in plot_combinations:
        process_and_plot(df, comp_type, func_type)

    # 2. NEU: Direkter Vergleich aller Compression Types für COMPRESS
    print("\n--- Erstelle globale Vergleichs-Diagramme für 'Compress' ---")
    #plot_function_time_comparison(df, 'Compress')
    #plot_function_size_comparison(df)


    # 3. NEU: Direkter Vergleich aller Compression Types für DECOMPRESS
    print("\n--- Erstelle globale Vergleichs-Diagramme für 'Decompress' ---")
    #plot_function_time_comparison(df, 'Decompress')
    # 4. Alle erstellten Diagramme anzeigen
    print("\nAlle Diagramme wurden erstellt und werden nun angezeigt.")
    plt.show()

if __name__ == "__main__":
    main()