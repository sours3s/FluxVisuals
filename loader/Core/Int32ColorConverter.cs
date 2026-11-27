using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace FluxVisualsLoader.Core;

/// <summary>
/// Цвета хранятся как ARGB Int32. Старый UI мог отправить то же значение как
/// беззнаковое число или hex-строку — converter приводит их к Int32 без потери битов.
/// </summary>
public sealed class Int32ColorConverter : JsonConverter<int>
{
    public override int Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number)
        {
            if (reader.TryGetInt32(out int signed)) return signed;
            if (reader.TryGetUInt32(out uint unsigned)) return unchecked((int)unsigned);
            if (reader.TryGetInt64(out long longValue)) return unchecked((int)longValue);
            throw new JsonException("Expected a 32-bit ARGB value");
        }

        if (reader.TokenType == JsonTokenType.String)
        {
            string? text = reader.GetString()?.Trim().TrimStart('#');
            if (!string.IsNullOrWhiteSpace(text) &&
                uint.TryParse(text, NumberStyles.HexNumber, CultureInfo.InvariantCulture, out uint hex))
            {
                // 6-значный hex из color input — это RGB, альфа добавляется здесь.
                if (text.Length == 6) hex |= 0xFF000000u;
                return unchecked((int)hex);
            }
        }

        throw new JsonException("Expected a 32-bit ARGB value");
    }

    public override void Write(Utf8JsonWriter writer, int value, JsonSerializerOptions options)
    {
        writer.WriteNumberValue(value);
    }
}
