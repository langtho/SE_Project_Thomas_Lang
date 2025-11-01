import pandas as pd
import json
import matplotlib.pyplot as plt
import numpy as np
from scipy.interpolate import make_interp_spline
import seaborn as sns 
import os
import io

# --- Utility Dictionaries and Definitions ---
VALUE_COLORS = {
    'small_v': 'blue', 
    'small_medium_v': 'cyan', 
    'medium_v': 'green', 
    'medium_large_v': 'orange', 
    'large_v': 'red', 
    'mixed_v': 'purple',
    'small_large_mix' :'pink'
}
COMPRESSION_STYLES = {
    'NonSpanning': 'solid', 
    'Spanning': 'dashed', 
    'Overflow': 'dotted'
}
ARRAY_SIZE_ORDER = ['small_s', 'small_medium_s', 'medium_s', 'medium_large_s', 'large_s']

# --- Global Comparison Plots (Comparing all 3 compression types) ---

def plot_simplified_time_comparison(df, function_type):
    """
    Compares the AVERAGE runtime (Y) of all compression types for a given function_type,
    AGGREGATING over all 'valueSize' categories. Reduces the plot to 3 lines.
    """
    
    # 1. Filter data (only selected functionType and positive times)
    filtered_df = df[(df['functionType'] == function_type) & (df['fullDurationNanos'] > 0)].copy()
    if filtered_df.empty:
        print(f"No valid data for simplified comparison of function type '{function_type}'.")
        return
        
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    
    avg_df = filtered_df.groupby(['uncompressedArraySize', 'compressionType'])['fullTimeMillis'].mean().reset_index()

    plt.figure(figsize=(12, 8))
    
    comp_colors = {'NonSpanning': 'blue', 'Spanning': 'green', 'Overflow': 'red'}
    comp_styles = {'NonSpanning': 'solid', 'Spanning': 'dashed', 'Overflow': 'dotted'}
    
    for comp_type in avg_df['compressionType'].unique():
        subset = avg_df[avg_df['compressionType'] == comp_type]
            
        if not subset.empty and len(subset) >= 2:
            # Smoothing for better readability (optional, but recommended for clean trends)
            X_data = subset['uncompressedArraySize'].values
            Y_data = subset['fullTimeMillis'].values
            
            try:
                # Spline interpolation
                spl = make_interp_spline(X_data, Y_data, k=3)
                X_smooth = np.linspace(X_data.min(), X_data.max(), 500)
                Y_smooth = spl(X_smooth)
                
                plt.plot(
                    X_smooth, 
                    Y_smooth, 
                    color=comp_colors.get(comp_type, 'gray'), 
                    linestyle=comp_styles.get(comp_type, 'solid'),
                    linewidth=3,
                    label=f'{comp_type} (Avg. over all Value Sizes)'
                )
            except ValueError:
                # Fallback if smoothing fails (e.g., not enough unique X values)
                 plt.plot(
                    X_data, 
                    Y_data, 
                    color=comp_colors.get(comp_type, 'gray'), 
                    linestyle=comp_styles.get(comp_type, 'solid'),
                    linewidth=3,
                    alpha=0.6,
                    label=f'{comp_type} (Avg. over all Value Sizes) - Unsmoothed'
                )

    plt.ylim(bottom=0)
    
    positive_times = filtered_df['fullTimeMillis']
    max_y_limit = np.percentile(positive_times, 95) * 1.05 if not positive_times.empty else None
    if max_y_limit is not None:
         plt.ylim(top=max_y_limit)

    plt.xlabel('Uncompressed Array Size', fontsize=12)
    plt.ylabel('Average Time (ms)', fontsize=12)
    plt.title(f'Simplified Time Comparison (Average over all Value Sizes) for: {function_type}', fontsize=16)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Compression Type', loc='upper left')
    plt.tight_layout()

def plot_function_time_comparison(df, function_type):
    """
    Compares the runtime (Y) of all compression types for a given function_type.
    Each compression type is a line, colored by 'valueSize'.
    """
    
    # 1. Filter and clean data (only selected functionType and positive times)
    filtered_df = df[(df['functionType'] == function_type) & (df['fullDurationNanos'] > 0)].copy()
    if filtered_df.empty:
        print(f"No valid data for function type comparison '{function_type}'.")
        return
        
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    
    # 2. Aggregate: Average time per array size, compression type, and value size
    avg_df = filtered_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['fullTimeMillis'].mean().reset_index()

    plt.figure(figsize=(14, 9))
    
    # Define colors (Value Size) and line styles (Compression Type)
    
    # 3. Iterate and Plot Lines
    for comp_type, style in COMPRESSION_STYLES.items():
        for value_size, color in VALUE_COLORS.items():
            subset = avg_df[
                (avg_df['compressionType'] == comp_type) & 
                (avg_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) >= 2:
                # Plot the unsmoothed average line
                plt.plot(
                    subset['uncompressedArraySize'], 
                    subset['fullTimeMillis'], 
                    color=color, 
                    linestyle=style,
                    linewidth=2,
                    alpha=0.8,
                    label=f'{comp_type} ({value_size})'
                )

    # 4. Axis Configuration
    plt.ylim(bottom=0)
    
    # Dynamic Y-axis limit to the 95th percentile
    positive_times = filtered_df['fullTimeMillis']
    max_y_limit = np.percentile(positive_times, 95) * 1.05 if not positive_times.empty else None
    if max_y_limit is not None:
         plt.ylim(top=max_y_limit)

    plt.xlabel('Uncompressed Array Size', fontsize=12) 
    plt.ylabel('Average Time (ms)', fontsize=12)
    plt.title(f'Comparison of Compression Types for: {function_type} (Time vs. Size)', fontsize=16)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Compression / Value Size', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()


def plot_function_size_comparison(df):
    """
    Compares the compressed size of all compression types vs. array size (Compress only).
    """
    
    # Filter only Compress data
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty:
        print("No Compress data found for size comparison.")
        return
        
    # Aggregate: Average size per array size, compression type, and value size
    avg_df = compress_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['compressedArraySize'].mean().reset_index()

    plt.figure(figsize=(14, 9))
    max_size = compress_df['uncompressedArraySize'].max()
    
    # 1. Zero-Compression Line (as reference)
    plt.plot([0, max_size], [0, max_size], color='black', linestyle='--', label='Zero compression line (Baseline)')
    
    # 2. Iterate and Plot Lines
    for comp_type, style in COMPRESSION_STYLES.items():
        for value_size, color in VALUE_COLORS.items():
            subset = avg_df[
                (avg_df['compressionType'] == comp_type) & 
                (avg_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) >= 2:
                # Plot the unsmoothed average line
                plt.plot(
                    subset['uncompressedArraySize'], 
                    subset['compressedArraySize'], 
                    color=color, 
                    linestyle=style,
                    linewidth=2,
                    alpha=0.8,
                    label=f'{comp_type} ({value_size})'
                )

    # 3. Axis Configuration
    plt.xlabel('Uncompressed Array Size', fontsize=12)
    plt.ylabel('Average Compressed Size', fontsize=12)
    plt.title('Comparison of Compression Types: Compressed Size vs. Array Size', fontsize=16)
    plt.grid(True, which="major", linestyle='--', alpha=0.6)
    plt.legend(title='Compression / Value Size', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()


def plot_time_comparison_by_array_category(df, function_type):
    """
    Compares the average runtime of all compression types grouped by the CATEGORICAL array size.
    Uses a grouped bar chart with compression types as groups and value sizes as bars.
    """
    
    filtered_df = df[(df['functionType'] == function_type) & (df['fullDurationNanos'] > 0)].copy()
    if filtered_df.empty:
        print(f"No valid data for categorical time comparison '{function_type}'.")
        return

    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    
    # Aggregate: Average time per categorical array size, compression type, and value size
    avg_df = filtered_df.groupby(['arraySize', 'compressionType'])['fullTimeMillis'].mean().reset_index()

    # Reindex for plotting order
    avg_df['arraySize'] = pd.Categorical(avg_df['arraySize'], categories=ARRAY_SIZE_ORDER, ordered=True)
    avg_df = avg_df.sort_values('arraySize')

    plt.figure(figsize=(14, 8))
    # Use seaborn for a clear grouped bar chart
    sns.barplot(
        x='arraySize', 
        y='fullTimeMillis', 
        hue='compressionType', 
        data=avg_df, 
        palette={'NonSpanning': 'blue', 'Spanning': 'green', 'Overflow': 'red'}
    )
    
    plt.title(f'Average Time Comparison by Array Size Category for: {function_type}', fontsize=16)
    plt.xlabel('Array Size Category', fontsize=12)
    plt.ylabel('Average Time (ms)', fontsize=12)
    plt.xticks(rotation=0)
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.legend(title='Compression Type', loc='upper right')
    plt.tight_layout()


def plot_compression_ratio(df):
    """
    Plots the average compression ratio vs. uncompressed array size.
    Shows smoothed lines for each compression type, colored by 'valueSize'.
    """
    # Filter only Compress data
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty:
        print("No Compress data found for the compression ratio chart.")
        return

    # Calculate compression ratio (Ratio: 1 = perfect compression, 0 = no reduction)
    compress_df['ratio'] = 1 - (compress_df['compressedArraySize'] / compress_df['uncompressedArraySize'])
    
    # Group for the average
    avg_ratio_df = compress_df.groupby(['uncompressedArraySize', 'compressionType', 'valueSize'])['ratio'].mean().reset_index()

    plt.figure(figsize=(12, 8))
    
    # 1. Plot the lines
    for comp_type, style in COMPRESSION_STYLES.items():
        for value_size, color in VALUE_COLORS.items():
            subset = avg_ratio_df[
                (avg_ratio_df['compressionType'] == comp_type) & 
                (avg_ratio_df['valueSize'] == value_size)
            ]
            
            if not subset.empty and len(subset) > 2:
                # Smoothing logic (Spline interpolation for clearer trends)
                X_data = subset['uncompressedArraySize'].values
                Y_data = subset['ratio'].values
                
                spl = make_interp_spline(X_data, Y_data, k=3)
                X_smooth = np.linspace(X_data.min(), X_data.max(), 500)
                Y_smooth = spl(X_smooth)
                
                plt.plot(
                    X_smooth, 
                    Y_smooth, 
                    color=color, 
                    linestyle=style,
                    linewidth=2,
                    label=f'{comp_type} ({value_size})'
                )

    plt.ylim(0, 1.05) # Ratio is between 0 and 1
    plt.xlabel('Uncompressed Array Size', fontsize=12) 
    plt.ylabel('Average Compression Ratio (0-1)', fontsize=12)
    plt.title('Compression Ratio vs. Array Size (Effectiveness)', fontsize=14)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Compression / Value Size', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()


def plot_compression_ratio_by_value_category(df):
    """
    Compares the average compression ratio of all compression types grouped by the VALUE size category.
    Uses a grouped bar chart with compression types as groups and array sizes as bars.
    """
    
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty:
        print("No Compress data found for the categorical ratio chart.")
        return

    compress_df['ratio'] = 1 - (compress_df['compressedArraySize'] / compress_df['uncompressedArraySize'])
    
    # Aggregate: Average ratio per value size category and compression type
    avg_ratio_df = compress_df.groupby(['valueSize', 'compressionType'])['ratio'].mean().reset_index()

    # Reindex for plotting order
    value_order = list(VALUE_COLORS.keys())
    avg_ratio_df['valueSize'] = pd.Categorical(avg_ratio_df['valueSize'], categories=value_order, ordered=True)
    avg_ratio_df = avg_ratio_df.sort_values('valueSize')

    plt.figure(figsize=(14, 8))
    # Use seaborn for a clear grouped bar chart
    sns.barplot(
        x='valueSize', 
        y='ratio', 
        hue='compressionType', 
        data=avg_ratio_df, 
        palette={'NonSpanning': 'blue', 'Spanning': 'green', 'Overflow': 'red'}
    )
    
    plt.title('Average Compression Ratio Comparison by Value Size Category', fontsize=16)
    plt.xlabel('Value Size Category', fontsize=12)
    plt.ylabel('Average Compression Ratio (0-1)', fontsize=12)
    plt.xticks(rotation=45, ha='right')
    plt.ylim(0, 1.05)
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.legend(title='Compression Type', loc='upper right')
    plt.tight_layout()


def plot_efficiency(df):
    """
    Plots Compression Ratio (Y) vs. Total Time (X). 
    Points are colored by 'compressionType' and scaled by 'uncompressedArraySize'.
    """
    compress_df = df[df['functionType'] == 'Compress'].copy()
    if compress_df.empty: 
        print("No Compress data found for the efficiency chart.")
        return

    # Calculate compression ratio
    compress_df['ratio'] = 1 - (compress_df['compressedArraySize'] / compress_df['uncompressedArraySize'])
    
    # Define colors for compression types
    color_map = {'NonSpanning': 'blue', 'Spanning': 'green', 'Overflow': 'red'}
    
    plt.figure(figsize=(12, 8))
    
    # Scale point size based on array size
    min_size = compress_df['uncompressedArraySize'].min()
    max_size = compress_df['uncompressedArraySize'].max()
    compress_df['size_scaled'] = 20 + 180 * ((compress_df['uncompressedArraySize'] - min_size) / (max_size - min_size))

    for comp_type, color in color_map.items():
        subset = compress_df[compress_df['compressionType'] == comp_type]
        if not subset.empty:
            plt.scatter(
                subset['fullTimeMillis'], 
                subset['ratio'], 
                s=subset['size_scaled'], # Size of points
                color=color, 
                alpha=0.6,
                label=comp_type
            )
            
    plt.xlim(left=0) # Time starts at 0
    plt.ylim(0, 1.05) # Ratio is between 0 and 1
    
    plt.xlabel('Total Time (ms) (Lower is Better)', fontsize=12)
    plt.ylabel('Compression Ratio (0-1) (Higher is Better)', fontsize=12)
    plt.title('Efficiency: Compression Ratio vs. Time (Trade-off)', fontsize=14)
    plt.grid(True, linestyle='--', alpha=0.5)
    
    # Add legend for point size (Array Size)
    legend_sizes = [min_size, (min_size + max_size) / 2, max_size]
    legend_labels = ['Small', 'Medium', 'Large']
    
    scatter_handles = []
    for size, label in zip(legend_sizes, legend_labels):
        scaled_size = 20 + 180 * ((size - min_size) / (max_size - min_size))
        scatter_handles.append(plt.scatter([], [], c='gray', alpha=0.6, s=scaled_size, label=f'{label}'))
    
    # Separate legend for array size
    legend1 = plt.legend(scatter_handles, legend_labels, frameon=True, labelspacing=1, title='Array Size', loc='upper right')
    
    # Separate legend for compression type
    legend_handles = [plt.scatter([], [], c=color_map[ct], alpha=0.6, s=100, label=ct) for ct in color_map]
    plt.legend(legend_handles, list(color_map.keys()), title='Compression Type', loc='lower right')
    plt.gca().add_artist(legend1) # Add the first legend back
    
    plt.tight_layout()

# --- Single Compression Type Plots ---

def plot_category_heatmap(df, compression_type, function_type):
    """
    Plots a heatmap of average time grouped by arraySize and valueSize.
    """
    # Filter for the selected type
    filtered_df = df[
        (df['compressionType'] == compression_type) & 
        (df['functionType'] == function_type)
    ].copy()

    if filtered_df.empty: return

    # Calculate the average time for each category combination
    pivot_df = filtered_df.groupby(['arraySize', 'valueSize'])['fullTimeMillis'].mean().unstack()

    if pivot_df.empty:
        print(f"No sufficient data for heatmap for {compression_type} ({function_type}).")
        return

    # Define the order of axes for better readability
    value_order = list(VALUE_COLORS.keys())
    
    pivot_df = pivot_df.reindex(index=ARRAY_SIZE_ORDER, columns=value_order).dropna(axis=0, how='all').dropna(axis=1, how='all')

    plt.figure(figsize=(10, 7))
    # Use Seaborn for an aesthetic heatmap
    sns.heatmap(
        pivot_df, 
        annot=True,     # Show values in the plot
        fmt=".2f",      # Format to 2 decimal places
        cmap="YlOrRd",  # Color Map from Yellow to Red (Red = slow)
        linewidths=.5,  # Lines between cells
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
    Uses a linear Y-axis and dynamically limits the Y-axis to the 95th percentile.
    """
    
    plt.figure(figsize=(12, 8))
    
    # 1. Iterate over each 'valueSize' for the Scatter Plot
    for value_size, color in VALUE_COLORS.items():
        subset = df[df['valueSize'] == value_size]
        
        if not subset.empty:
            plt.scatter(
                subset['uncompressedArraySize'], 
                subset['fullTimeMillis'], 
                color=color, 
                alpha=0.6,
                label=f'Value Size: {value_size}'
            )
            
    # *** Dynamic Y-Axis Limit ***
    positive_times = df['fullTimeMillis'][df['fullTimeMillis'] > 0]
    
    if not positive_times.empty:
        percentile_95 = np.percentile(positive_times, 95)
        max_y_limit = percentile_95 * 1.05
        plt.ylim(bottom=0, top=max_y_limit)
        print(f"  Y-axis limited to: {max_y_limit:.2f} ms (95th Percentile: {percentile_95:.2f} ms)")
    else:
        plt.ylim(bottom=0)
            
    plt.xlabel('Uncompressed Array Size', fontsize=12) 
    plt.ylabel('Time (ms)', fontsize=12)
    plt.title(f'Time vs. Array-Size for {compression_type} ({function_type}) (Limited Linear Time Scale)', fontsize=14)
    plt.grid(True, which="major", linestyle='--', alpha=0.5)
    plt.legend(title='Value Size Category', loc='upper left')
    plt.tight_layout()

def plot_compressed_size_vs_array_size(df, compression_type):
    """
    Plots a scatter plot of compressed size vs. uncompressed array size with a zero-compression line.
    """
    plt.figure(figsize=(10, 6))
    max_size = df['uncompressedArraySize'].max()
    # Zero-Compression: Compressed Size = Uncompressed Size
    plt.plot([0, max_size], [0, max_size], color='black', linestyle='--', label='Zero compression line')
    
    for value_size, color in VALUE_COLORS.items():
            subset = df[df['valueSize'] == value_size]
            if not subset.empty:
                plt.scatter(
                    subset['uncompressedArraySize'], 
                    subset['compressedArraySize'], 
                    color=color, 
                    alpha=0.7,
                    label=f'Value Size: {value_size}',
                    zorder=2 # Draw points above the black line
                )
    plt.xlabel('Uncompressed Array Size', fontsize=12)
    plt.ylabel('Compressed Size', fontsize=12)
    plt.title(f'Compressed Size vs. Uncompressed Array-Size for {compression_type}', fontsize=14)
    plt.grid(True, linestyle='--', alpha=0.6)
    plt.legend(title='Value Size Category')
    plt.tight_layout()

def plot_stacked_bar_chart(df, compression_type, function_type):
    """
    Plots a stacked bar chart of average part times grouped by the categorical array size ('arraySize').
    Adds the total average time above each bar as a text annotation.
    """
    parts_data_list = []
    
    # 1. Collect all 'parts' data and keep the 'arraySize' category
    for _, row in df.iterrows():
        if isinstance(row.get('parts'), list):
            for part in row['parts']:
                parts_data_list.append({
                    'arraySize': row['arraySize'],
                    'partName': part['name'],
                    'timeNanos': part['timeNanos']
                })
    
    if not parts_data_list:
        print(f"  Warning: No 'part' data found for {compression_type} ({function_type}). Skipping chart.")
        return
        
    parts_df = pd.DataFrame(parts_data_list)
    
    # 2. Filter and define the X-axis order
    parts_df = parts_df[parts_df['arraySize'].isin(ARRAY_SIZE_ORDER)]
    
    # Calculate average times per CATEGORICAL array size and part
    avg_times = parts_df.groupby(['arraySize', 'partName'])['timeNanos'].mean().reset_index()
    avg_times['timeMillis'] = avg_times['timeNanos'] / 1_000_000
    
    # 3. Create Pivot Table
    pivot_df = avg_times.pivot_table(
        index='arraySize', 
        columns='partName', 
        values='timeMillis', 
        aggfunc='sum'
    ).fillna(0)
    
    # 4. Apply the defined order to the index
    plot_order = [size for size in ARRAY_SIZE_ORDER if size in pivot_df.index]
    pivot_df = pivot_df.reindex(plot_order)
    
    # Calculate Total Average Time
    total_avg_time = pivot_df.sum(axis=1)
    
    # 5. Plotting
    ax = pivot_df.plot(kind='bar', stacked=True, figsize=(12, 8))
    
    # Add text annotations for total time
    for i, total in enumerate(total_avg_time.values):
        ax.text(
            x=i, 
            y=total + total_avg_time.max() * 0.01, # Small offset above the bar
            s=f'{total:.2f} ms', # Total time (e.g., "12.34 ms")
            ha='center', 
            va='bottom', 
            fontsize=10,
            fontweight='bold'
        )

    plt.title(f'Average time for {compression_type} ({function_type}) (Total Time Annotated)', fontsize=16)
    plt.xlabel('Array Size Category', fontsize=12)
    plt.ylabel('Average time (ms)', fontsize=12)
    plt.xticks(rotation=0) 
    plt.legend(title='Parts', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    plt.subplots_adjust(top=0.9) # Adjust space for annotations


# ----------------------------------------------------------------------
# MODULAR PLOTTING EXECUTION
# ----------------------------------------------------------------------

def process_and_plot(df, compression_type, function_type, individual_plots):
    """
    Filters the DataFrame, calculates time in milliseconds, and generates 
    relevant plots for the specified compression and function type combination.
    """
    
    # Filter DataFrame
    filtered_df = df[
        (df['compressionType'] == compression_type) & 
        (df['functionType'] == function_type)
    ].copy()

    if filtered_df.empty:
        print(f"No data found for compressionType '{compression_type}' and functionType '{function_type}'.")
        return
    
    print(f"\n--- Generating Charts for: {compression_type} - {function_type} ---")
    
    # Calculate total time in milliseconds
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000

    # Execute individual plots based on the list
    if 'time_vs_size' in individual_plots:
        plot_time_vs_array_size(filtered_df, compression_type, function_type)
    
    if function_type == 'Compress' and 'compressed_size_vs_size' in individual_plots:
        plot_compressed_size_vs_array_size(filtered_df, compression_type)
    
    if 'stacked_bar' in individual_plots:
        plot_stacked_bar_chart(filtered_df, compression_type, function_type)

    if 'heatmap' in individual_plots:
        plot_category_heatmap(filtered_df, compression_type, function_type)


def main():
    # Define the file path
    file_path = '../src/main/resources/performance_data.jsonl'
    df = None 

    # --- 1. Data Loading ---
    try:
        print(f"Reading data from: {file_path}")
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = [line.strip() for line in f if line.strip()]
        parsed_data = [json.loads(line) for line in lines]
        df = pd.DataFrame(parsed_data)
        print(f"Data successfully loaded. {len(df)} rows found.")
    except FileNotFoundError:
        print(f"Error: The file '{file_path}' does not exist.")
        return
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON in line: {e.lineno}. Error: {e.msg}")
        return
        
    if df is None or df.empty:
        print("The loaded DataFrame is empty. No charts can be generated.")
        return

    if 'fullDurationNanos' in df.columns:
        df['fullTimeMillis'] = df['fullDurationNanos'] / 1_000_000
    else:
        print("Error: 'fullDurationNanos' column not found. Cannot calculate 'fullTimeMillis'.")
        return
    # --- Configuration of Plots to Generate ---
    
    # Define which *individual* plots should be generated for each combination
    # Options: 'time_vs_size', 'compressed_size_vs_size', 'stacked_bar', 'heatmap'
    INDIVIDUAL_PLOTS = ['time_vs_size', 'compressed_size_vs_size', 'stacked_bar', 'heatmap']
    
    # Define which *global* comparison plots should be generated
    # Options: 'time_comparison', 'size_comparison', 'efficiency', 'ratio_vs_size', 'ratio_vs_value'
    GLOBAL_PLOTS = ['time_comparison', 'size_comparison', 'efficiency', 'ratio_vs_size', 'ratio_vs_value', 'time_comparison_by_category']
    
    # Define the combinations for which *individual* plots should be generated
    PLOT_COMBINATIONS = [
        # ('CompressionType', 'FunctionType')
        ('NonSpanning', 'Compress'),
        ('Spanning', 'Compress'),
        ('Overflow', 'Compress'),
        
        ('NonSpanning', 'Decompress'),
        ('Spanning', 'Decompress'),
        ('Overflow', 'Decompress'),
        
        ('NonSpanning', 'get'),
        ('Spanning', 'get'),
        ('Overflow', 'get'),
    ]

    # --- 2. Iteration and Execution of Individual Plots ---
    for comp_type, func_type in PLOT_COMBINATIONS:
        process_and_plot(df, comp_type, func_type, INDIVIDUAL_PLOTS)

    # --- 3. Execution of Global Comparison Plots ---
    
    if 'time_comparison' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Time vs. Size (Compress) ---")
        plot_simplified_time_comparison(df, 'Compress')
        print("--- Generating Global Comparison Chart: Time vs. Size (Decompress) ---")
        plot_simplified_time_comparison(df, 'Decompress')
        
    if 'size_comparison' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Compressed Size vs. Size ---")
        plot_function_size_comparison(df)
        
    if 'efficiency' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Efficiency (Ratio vs. Time) ---")
        plot_efficiency(df)
        
    if 'ratio_vs_size' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Ratio vs. Array Size ---")
        plot_compression_ratio(df)

    if 'ratio_vs_value' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Ratio vs. Value Category ---")
        plot_compression_ratio_by_value_category(df)

    if 'time_comparison_by_category' in GLOBAL_PLOTS:
        print("\n--- Generating Global Comparison Chart: Time by Array Category (Compress) ---")
        plot_time_comparison_by_array_category(df, 'Compress')
        print("--- Generating Global Comparison Chart: Time by Array Category (Decompress) ---")
        plot_time_comparison_by_array_category(df, 'Decompress')
        
    # --- 4. Display all generated charts ---
    print("\nAll charts have been created and are now being displayed.")
    plt.show()

if __name__ == "__main__":
    main()