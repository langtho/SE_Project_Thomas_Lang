import pandas as pd
import json
import matplotlib.pyplot as plt
import os
import io


def plot_time_vs_array_size(df, compression_type, function_type):
    """
    Plots a scatter plot of total time vs. uncompressed array size.
    """
    plt.figure(figsize=(10, 6))
    plt.scatter(df['uncompressedArraySize'], df['fullTimeMillis'], color='blue', alpha=0.7)
    plt.xlabel('Array-Size', fontsize=12)
    plt.ylabel('Time (ms)', fontsize=12)
    plt.title(f'Time vs. Array-Size for {compression_type} ({function_type})', fontsize=14)
    plt.grid(True)
    plt.tight_layout()

def plot_compressed_size_vs_array_size(df, compression_type):
    """
    Plots a scatter plot of compressed size vs. uncompressed array size with a zero-compression line.
    """
    plt.figure(figsize=(10, 6))
    max_size = df['uncompressedArraySize'].max()
    plt.plot([0, max_size], [0, max_size], color='red', linestyle='--', label='Zero compression line')
    plt.scatter(df['uncompressedArraySize'], df['compressedArraySize'], color='blue', alpha=0.7)
    plt.xlabel('Array-Size', fontsize=12)
    plt.ylabel('Compressed Size', fontsize=12)
    plt.title(f'Compressed Size vs. Uncompressed Array-Size for {compression_type}', fontsize=14)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()

def plot_stacked_bar_chart(df, compression_type, function_type):
    """
    Plots a stacked bar chart of average part times grouped by array size.
    """
    parts_data_list = []
    for _, row in df.iterrows():
        for part in row['parts']:
            parts_data_list.append({
                'uncompressedArraySize': row['uncompressedArraySize'],
                'partName': part['name'],
                'timeNanos': part['timeNanos']
            })
    
    if not parts_data_list:
        print("No part data found for plotting.")
        return
        
    parts_df = pd.DataFrame(parts_data_list)
    avg_times = parts_df.groupby(['uncompressedArraySize', 'partName'])['timeNanos'].mean().reset_index()
    avg_times['timeMillis'] = avg_times['timeNanos'] / 1_000_000
    
    pivot_df = avg_times.pivot_table(
        index='uncompressedArraySize', 
        columns='partName', 
        values='timeMillis', 
        aggfunc='sum'
    ).fillna(0)
    
    pivot_df.plot(kind='bar', stacked=True, figsize=(12, 8))
    plt.title(f'Average time for {compression_type} ({function_type})', fontsize=16)
    plt.xlabel('Original Array-Size', fontsize=12)
    plt.ylabel('Average time (ms)', fontsize=12)
    plt.xticks(rotation=0)
    plt.legend(title='Parts', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()

def main():
    # Path to jsonl file
    file_path = '../src/main/resources/performance_data.jsonl'

    # Load data from jsonl file
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = [line.strip() for line in f if line.strip()]
        parsed_data = [json.loads(line) for line in lines]
        df = pd.DataFrame(parsed_data)
    except FileNotFoundError:
        print(f"Error: The file '{file_path}' does not exist.")
        return
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON: {e}")
        return

    selected_compression_type = 'NonSpanning'
    selected_function_type = 'Compress'
    
    filtered_df = df[(df['compressionType'] == selected_compression_type) & (df['functionType'] == selected_function_type)].copy()

    if filtered_df.empty:
        print(f"No data found for compressionType '{selected_compression_type}' and functionType '{selected_function_type}'.")
        return
    
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    
    plot_time_vs_array_size(filtered_df, selected_compression_type, selected_function_type)
    plot_compressed_size_vs_array_size(filtered_df, selected_compression_type)
    plot_stacked_bar_chart(filtered_df, selected_compression_type, selected_function_type)
    
    
    selected_compression_type = 'Spanning'
    selected_function_type = 'Compress'
    
    filtered_df = df[(df['compressionType'] == selected_compression_type) & (df['functionType'] == selected_function_type)].copy()

    if filtered_df.empty:
        print(f"No data found for compressionType '{selected_compression_type}' and functionType '{selected_function_type}'.")
        return
    
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    plot_time_vs_array_size(filtered_df, selected_compression_type, selected_function_type)
    plot_compressed_size_vs_array_size(filtered_df, selected_compression_type)
    plot_stacked_bar_chart(filtered_df, selected_compression_type, selected_function_type)

    selected_compression_type = 'Overflow'
    selected_function_type = 'Compress'
    
    filtered_df = df[(df['compressionType'] == selected_compression_type) & (df['functionType'] == selected_function_type)].copy()

    if filtered_df.empty:
        print(f"No data found for compressionType '{selected_compression_type}' and functionType '{selected_function_type}'.")
        return
    
    filtered_df['fullTimeMillis'] = filtered_df['fullDurationNanos'] / 1_000_000
    plot_time_vs_array_size(filtered_df, selected_compression_type, selected_function_type)
    plot_compressed_size_vs_array_size(filtered_df, selected_compression_type)
    plot_stacked_bar_chart(filtered_df, selected_compression_type, selected_function_type)
    
    
    plt.show()

if __name__ == "__main__":
    main()