package com.csvconvert;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "csv-convert",
        mixinStandardHelpOptions = true,
        version = "csv-convert 1.0.0",
        description = {
                "CSVの対象カラムを抽出します。デフォルトは元の並びのままです（--reverse で逆順）。",
                "カラム名は最後のヘッダ行から解決します。番号指定は1始まりです（例: 1,3 や #1,#3）。"
        }
)
public class Cli implements Callable<Integer> {
    @Option(
            names = {"-c", "--columns"},
            split = ",",
            paramLabel = "<column>",
            description = "抽出するカラム。ヘッダ名または1始まりの番号。省略時は全カラム。"
    )
    List<String> columns = new ArrayList<>();

    @Option(
            names = {"-e", "--encoding"},
            paramLabel = "<charset>",
            description = "入出力の文字コード。デフォルト: ${DEFAULT-VALUE}"
    )
    String encoding = "UTF-8";

    @Option(
            names = {"--input-encoding"},
            paramLabel = "<charset>",
            description = "入力の文字コード。省略時は --encoding を使う。"
    )
    String inputEncoding;

    @Option(
            names = {"--output-encoding"},
            paramLabel = "<charset>",
            description = "出力の文字コード。省略時は --encoding を使う。"
    )
    String outputEncoding;

    @Option(
            names = {"-d", "--delimiter"},
            paramLabel = "<char>",
            description = "区切り文字。1文字、または TAB / COMMA / SEMI / PIPE / SPACE。デフォルト: ,"
    )
    String delimiter = ",";

    @Option(
            names = {"-q", "--quote"},
            paramLabel = "<char>",
            description = "囲み文字。1文字、または DOUBLE / SINGLE / NONE。デフォルト: \""
    )
    String quote = "\"";

    @Option(
            names = {"-H", "--header-rows"},
            paramLabel = "<n>",
            description = "ヘッダ行数。この行数は並び替え対象外。デフォルト: ${DEFAULT-VALUE}。0でヘッダなし。"
    )
    int headerRows = 1;

    @Option(
            names = {"--reverse"},
            negatable = true,
            description = "ヘッダ以外のデータ行を逆順にする。デフォルト: ${DEFAULT-VALUE}。"
    )
    boolean reverse = false;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "<file>",
            description = "出力ファイル。省略時は標準出力、または第2引数。"
    )
    Path output;

    @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "<input>",
            description = "入力CSVファイル。省略または - のときは標準入力。"
    )
    Path input;

    @Parameters(
            index = "1",
            arity = "0..1",
            paramLabel = "<output>",
            description = "出力CSVファイル。--output より優先される。"
    )
    Path positionalOutput;

    public static void main(String[] args) {
        ConsoleEncoding.install();
        System.exit(commandLine().execute(args));
    }

    static CommandLine commandLine() {
        return new CommandLine(new Cli())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    commandLine.getErr().println(ex.getMessage());
                    return 1;
                });
    }

    @Override
    public Integer call() throws Exception {
        ConvertOptions options = new ConvertOptions(
                columns.stream().map(String::strip).filter(value -> !value.isEmpty()).toList(),
                CsvChars.parseCharset(inputEncoding != null ? inputEncoding : encoding),
                CsvChars.parseCharset(outputEncoding != null ? outputEncoding : encoding),
                CsvChars.parseDelimiter(delimiter),
                CsvChars.parseQuote(quote),
                headerRows,
                reverse
        );

        Path outputPath = positionalOutput != null ? positionalOutput : output;
        boolean closeInput = input != null && !Path.of("-").equals(input);
        boolean closeOutput = outputPath != null;

        InputStream in = closeInput ? Files.newInputStream(input) : System.in;
        OutputStream out = closeOutput ? Files.newOutputStream(outputPath) : System.out;
        try {
            new CsvConverter().convert(in, out, options);
        } finally {
            if (closeInput) {
                in.close();
            }
            if (closeOutput) {
                out.close();
            }
        }
        return 0;
    }
}
