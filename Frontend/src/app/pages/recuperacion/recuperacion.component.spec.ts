import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { RecuperacionComponent } from './recuperacion.component';

const api = 'http://localhost:8080/api/auth/recuperacion';
const token = 'A'.repeat(43);
function crear(restablecer = false, enlace = token) {
  TestBed.configureTestingModule({ imports: [RecuperacionComponent],
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  const ruta = TestBed.inject(ActivatedRoute);
  ruta.snapshot.data = { restablecer };
  ruta.snapshot.fragment = 'token=' + enlace;
  const fixture = TestBed.createComponent(RecuperacionComponent);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  return { fixture, http };
}

function escribir(fixture: any, nombre: string, valor: string) {
  const campo = fixture.nativeElement.querySelector(`input[name="${nombre}"]`);
  campo.value = valor;
  campo.dispatchEvent(new Event('input', { bubbles: true }));
}

describe('Recuperación de contraseña', () => {
  it('solicita el enlace y muestra la respuesta genérica sin revelar cuentas', async () => {
    const { fixture, http } = crear();
    await fixture.whenStable();
    escribir(fixture, 'email', 'CLIENTE@EXAMPLE.COM');
    fixture.detectChanges(); await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const solicitud = http.expectOne(api + '/solicitar');
    expect(solicitud.request.body.email).toBe('cliente@example.com');
    solicitud.flush({ mensaje: 'Si el correo corresponde a una cuenta activa, recibirás un enlace.' }, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Si el correo corresponde');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    http.verify();
  });

  it('valida el enlace antes de permitir elegir una nueva contraseña', async () => {
    const { fixture, http } = crear(true);
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    http.expectOne(api + '/validar').flush({ valido: true });
    await fixture.whenStable();
    escribir(fixture, 'contrasenia', 'Mi nueva frase segura');
    escribir(fixture, 'confirmacion', 'Mi nueva frase segura');
    fixture.detectChanges(); await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const cambio = http.expectOne(api + '/restablecer');
    expect(cambio.request.body).toEqual({ token, contrasenia: 'Mi nueva frase segura', confirmacion: 'Mi nueva frase segura' });
    cambio.flush({ mensaje: 'Contraseña actualizada.' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Iniciar sesión con mi nueva contraseña');
    expect(fixture.componentInstance.contrasenia).toBe('');
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
    http.verify();
  });

  it('rechaza un enlace vencido y ofrece solicitar otro', async () => {
    const { fixture, http } = crear(true);
    http.expectOne(api + '/validar').flush({ message: 'El enlace es inválido, ya se utilizó o venció.' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('venció');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/recuperar-contrasena"]')).not.toBeNull();
    http.verify();
  });

  it('no envía contraseñas diferentes ni tokens incompletos', async () => {
    const { fixture, http } = crear(true);
    http.expectOne(api + '/validar').flush({ valido: true });
    await fixture.whenStable();
    escribir(fixture, 'contrasenia', 'Mi nueva frase segura');
    escribir(fixture, 'confirmacion', 'Otra frase diferente');
    fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    http.expectNone(api + '/restablecer');
    expect(fixture.nativeElement.textContent).toContain('Las contraseñas no coinciden.');
    http.verify();
  });

  it('un token con formato inválido no muestra el formulario', () => {
    const { fixture, http } = crear(true, 'incompleto');
    expect(fixture.nativeElement.textContent).toContain('incompleto');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    http.expectNone(api + '/validar');
    http.verify();
  });
});
