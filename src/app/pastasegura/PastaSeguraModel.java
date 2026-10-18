package app.pastasegura;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import model.ArquivoSeguro;
import model.Usuario;

public final class PastaSeguraModel {

  private final Usuario usuario;
  private Path pasta;
  private char[] fraseSecreta;
  private List<ArquivoSeguro> arquivos = Collections.emptyList();

  public PastaSeguraModel(Usuario usuario) {
    if (usuario == null) {
      throw new IllegalArgumentException(
          "A pasta segura exige um usuário autenticado.");
    }
    this.usuario = usuario;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Path getPasta() {
    return pasta;
  }

  public List<ArquivoSeguro> getArquivos() {
    return arquivos;
  }

  public void registrarConsulta(
      Path pasta,
      char[] fraseSecreta,
      List<ArquivoSeguro> arquivos) {
    if (pasta == null || fraseSecreta == null || arquivos == null) {
      throw new IllegalArgumentException(
          "Os dados da consulta da pasta segura são obrigatórios.");
    }
    limparFraseSecreta();
    this.pasta = pasta;
    this.fraseSecreta = fraseSecreta.clone();
    this.arquivos = Collections.unmodifiableList(
        new ArrayList<>(arquivos));
  }

  public char[] obterCopiaDaFraseSecreta() {
    return fraseSecreta == null ? null : fraseSecreta.clone();
  }

  public ArquivoSeguro buscarArquivo(String codigo) {
    for (ArquivoSeguro arquivo : arquivos) {
      if (arquivo.getCodigo().equals(codigo)) {
        return arquivo;
      }
    }
    return null;
  }

  public void limparConsulta() {
    limparFraseSecreta();
    pasta = null;
    arquivos = Collections.emptyList();
  }

  private void limparFraseSecreta() {
    if (fraseSecreta != null) {
      Arrays.fill(fraseSecreta, '\0');
      fraseSecreta = null;
    }
  }
}
